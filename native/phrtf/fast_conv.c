#include "fast_conv.h"
#include <stdio.h>
#include <string.h>

void *fast_conv_upols_init(uint32_t blockSize, float *h, uint32_t len)
{
    fconvStruct *S = (fconvStruct *)calloc(1, sizeof(fconvStruct));
    if (!S) {
        printf("Failed to calloc olsStruct *S in %s line %d.\n",
               "/home/zengqinglin/claude_work/spAudio/pHRTF/fast_conv.c", 0x1c);
        return NULL;
    }

    uint32_t validFFT = blockSize + 1;
    uint32_t nfft = blockSize * 2;
    uint32_t hBlocks = 0;
    if (blockSize != 0) {
        hBlocks = (len - 1) / blockSize;
    }
    hBlocks += 1;

    S->blocksize = blockSize;
    S->nfft = nfft;
    S->validFFT = validFFT;
    S->hBlocks = hBlocks;

    S->H = (kiss_fft_cpx *)malloc((size_t)hBlocks * validFFT * sizeof(kiss_fft_cpx));
    S->xFDL = (kiss_fft_cpx *)malloc((size_t)hBlocks * validFFT * sizeof(kiss_fft_cpx));
    if (S->xFDL) {
        memset(S->xFDL, 0, (size_t)hBlocks * validFFT * sizeof(kiss_fft_cpx));
    }

    S->FDLSidx = (int *)malloc((size_t)hBlocks * sizeof(int));
    for (uint32_t i = 0; i < hBlocks; i++) {
        S->FDLSidx[i] = (int)i;
    }

    size_t time_size = (size_t)nfft * sizeof(float);
    float *timedata = (float *)malloc(time_size);
    S->xTemp = (float *)malloc(time_size);
    if ((int)nfft > 0) {
        memset(timedata, 0, time_size);
        memset(S->xTemp, 0, time_size);
    }

    size_t h_padded_size = (size_t)hBlocks * blockSize * sizeof(float);
    float *hTemp = (float *)malloc(h_padded_size);
    if (hTemp) {
        memset(hTemp, 0, h_padded_size);
        memcpy(hTemp, h, (size_t)len * sizeof(float));
    }

    kiss_fft_cpx *freqdata = S->H;
    for (uint32_t b = 0; b < hBlocks; b++) {
        memcpy(timedata, hTemp + b * blockSize, (size_t)blockSize * sizeof(float));
        kiss_fftr_cfg hfft = kiss_fftr_alloc((int)nfft, 0, NULL, NULL);
        kiss_fftr(hfft, timedata, freqdata);
        free(hfft);
        freqdata += validFFT;
    }
    free(timedata);

    S->yTemp = (float *)malloc(time_size);
    S->yTempFFT = (kiss_fft_cpx *)malloc((size_t)validFFT * sizeof(kiss_fft_cpx));
    S->fft = kiss_fftr_alloc((int)nfft, 0, NULL, NULL);
    S->ifft = kiss_fftr_alloc((int)nfft, 1, NULL, NULL);

    free(hTemp);
    return S;
}

void fast_conv_upols(void *handle, float *pSrc, float *pDst)
{
    fconvStruct *S = (fconvStruct *)handle;
    uint32_t blockSize = S->blocksize;
    uint32_t nfft = S->nfft;
    uint32_t validFFT = S->validFFT;
    uint32_t hBlocks = S->hBlocks;

    memcpy(S->xTemp + blockSize, pSrc, (size_t)blockSize * sizeof(float));

    kiss_fftr(S->fft, S->xTemp, S->xFDL + (size_t)validFFT * S->FDLSidx[0]);

    if (hBlocks == 0) {
        memset(S->yTempFFT, 0, (size_t)validFFT * sizeof(kiss_fft_cpx));
    } else {
        for (uint32_t k = 0; k < validFFT; k++) {
            float sum_r = 0.0f;
            float sum_i = 0.0f;
            for (uint32_t b = 0; b < hBlocks; b++) {
                kiss_fft_cpx x = S->xFDL[S->FDLSidx[b] * validFFT + k];
                kiss_fft_cpx h = S->H[b * validFFT + k];
                sum_r += x.r * h.r - x.i * h.i;
                sum_i += x.r * h.i + x.i * h.r;
            }
            S->yTempFFT[k].r = sum_r;
            S->yTempFFT[k].i = sum_i;
        }
    }

    kiss_fftri(S->ifft, S->yTempFFT, S->yTemp);

    float inv_nfft = 1.0f / (float)nfft;
    for (uint32_t i = 0; i < nfft; i++) {
        S->yTemp[i] *= inv_nfft;
    }

    memcpy(pDst, S->yTemp + blockSize, (size_t)blockSize * sizeof(float));

    if ((int)hBlocks - 1 > 0) {
        int last = S->FDLSidx[hBlocks - 1];
        S->FDLStemp = last;
        for (int j = (int)hBlocks - 1; j > 0; j--) {
            S->FDLSidx[j] = S->FDLSidx[j - 1];
        }
        S->FDLSidx[0] = last;
    }

    int nextCounter = 0;
    if (S->blockCounter != (int)hBlocks - 2) {
        nextCounter = S->blockCounter + 1;
    }
    S->blockCounter = nextCounter;

    memcpy(S->xTemp, S->xTemp + blockSize, (size_t)blockSize * sizeof(float));
}

void fast_conv_upols_free(void *handle)
{
    fconvStruct *S = (fconvStruct *)handle;
    if (!S) return;
    free(S->xTemp);
    free(S->yTemp);
    free(S->xFDL);
    free(S->H);
    free(S->yTempFFT);
    free(S->fft);
    free(S->ifft);
    free(S->FDLSidx);
    free(S);
}
