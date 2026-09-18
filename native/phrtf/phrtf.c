#include "phrtf.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

#ifdef __cplusplus
extern "C" {
#endif

/* Forward declarations of functions in func.c and tables.S */
extern const float defaultHannWin[2048];
void *malloc_memset(int size);
void safe_release(void *pt);
void invlinearSweep(float *sweep, int fstart, int fend, float duration, int fs);
void invlogSweep(float *sweep, int T, float f1, float f2, float duration, int fs);
int peak_detection(float *audioData, int length, int sweeptype);
void biquad_df2t(float *coeff, float *z, float *data, uint32_t dataLen);
void mypwelch(void *handle, float *x, int N, int L, int D, float *P_hat);
void arrayMag2db(float *x);
void meloctsmooth(float *H, float *Hsm, int index);
int paraMatch(float *actual);
void convert1Dto2D(int indexStamp, int32_t **twoDArray, int oneDSize, int fs, int rows, int cols);

static const int32_t inputShift48k[17] = {
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
};

static const int32_t inputShift44k[17] = {
    1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1
};

void *phrtf_init(void)
{
    pHRTF *hrtfPara = (pHRTF *)malloc_memset(sizeof(pHRTF));
    if (!hrtfPara) {
        printf("Error: failed to calloc");
        return NULL;
    }

    hrtfPara->fs = 48000;
    hrtfPara->blockNum1 = 60;  /* 0x3c */
    hrtfPara->blockNum2 = 47;  /* 0x2f */

    hrtfPara->hDstEnergy = (float *)malloc_memset(3 * sizeof(float));
    if (hrtfPara->hDstEnergy) {
        /* Target energies: 2.25076e-05, 2.18844e-05, 4.58888e-06 */
        union { uint32_t u; float f; } e0 = { 0x37bcce77 };
        union { uint32_t u; float f; } e1 = { 0x37b79705 };
        union { uint32_t u; float f; } e2 = { 0x3699fb58 };
        hrtfPara->hDstEnergy[0] = e0.f;
        hrtfPara->hDstEnergy[1] = e1.f;
        hrtfPara->hDstEnergy[2] = e2.f;
    }

    hrtfPara->fft = kiss_fftr_alloc(1024, 0, NULL, NULL);
    hrtfPara->X = (kiss_fft_cpx *)malloc_memset(1025 * sizeof(kiss_fft_cpx));
    hrtfPara->Hsm = (float *)malloc_memset(15 * sizeof(float));
    hrtfPara->hannWin = (float *)malloc_memset(2048 * sizeof(float));
    memcpy(hrtfPara->hannWin, defaultHannWin, 2048 * sizeof(float));

    hrtfPara->x = (float *)malloc_memset(2048 * sizeof(float));

    if (hrtfPara->x != NULL) {
        int h1_samples = (int)((double)hrtfPara->fs * 0.2);
        hrtfPara->h1 = (float *)malloc_memset(h1_samples * sizeof(float));

        int h2_samples = hrtfPara->fs * 2;
        hrtfPara->h2 = (float *)malloc_memset(h2_samples * sizeof(float));

        invlinearSweep(hrtfPara->h1, 5000, 20000, 0.2f, hrtfPara->fs);
        invlogSweep(hrtfPara->h2, 96000, 1000.0f, 22050.0f, 2.0f, hrtfPara->fs);
        return hrtfPara;
    }

    printf("Error: failed to calloc");
    return NULL;
}

void single_channel_process(void *handle, float *pSrc, float *pDst)
{
    pHRTF *phrtfPara = (pHRTF *)handle;

    phrtfPara->fconvHandle1 = (fconvStruct *)fast_conv_upols_init(2048, phrtfPara->h1, 9600);
    phrtfPara->fconvHandle2 = (fconvStruct *)fast_conv_upols_init(2048, phrtfPara->h2, 96000);

    for (int i = 0; i < phrtfPara->blockNum1; i++) {
        fast_conv_upols(phrtfPara->fconvHandle1, pSrc + i * 2048, pDst + i * 2048);
    }

    int peak = peak_detection(pDst, 96000, 0);
    memset(pDst, 0, 768000); /* 0xbb800 bytes = 192000 floats */

    for (int i = 0; i < phrtfPara->blockNum2; i++) {
        fast_conv_upols(phrtfPara->fconvHandle2, pSrc + peak + i * 2048, pDst + i * 2048);
    }

    memcpy(pDst, pDst + 91904, 32768); /* 0x16700 floats offset, 0x8000 bytes = 8192 floats */
    memset(pDst + 8192, 0, 384000);    /* 0x5dc00 bytes = 96000 floats */

    fast_conv_upols_free(phrtfPara->fconvHandle1);
    fast_conv_upols_free(phrtfPara->fconvHandle2);
}

void energyMatch(void *handle, float *hOri, int angleIndex)
{
    pHRTF *para = (pHRTF *)handle;
    memcpy(para->x, hOri, 2048 * sizeof(float));

    float *P_hat = (float *)malloc(513 * sizeof(float));
    mypwelch(handle, hOri, 48000, 1024, 512, P_hat);
    memcpy(&para->absH[angleIndex][0], P_hat, 513 * sizeof(float));

    double energy = 0.0;
    for (int i = 0; i < 513; i++) {
        energy += (double)fabsf(P_hat[i]);
    }
    free(P_hat);

    float factor = (float)sqrt((double)para->hDstEnergy[angleIndex] / energy);
    for (int i = 0; i < 512; i++) {
        para->X[i].r *= factor;
        para->X[i].i *= factor;
        para->absH[angleIndex][i] *= factor;
    }
}

float hEnergy(void *handle, float *h, int index)
{
    pHRTF *para = (pHRTF *)handle;
    memcpy(para->x, h, 2048 * sizeof(float));

    float *P_hat = (float *)malloc(513 * sizeof(float));
    mypwelch(handle, h, 48000, 1024, 512, P_hat);
    memcpy(&para->absH[index][0], P_hat, 513 * sizeof(float));

    double energy = 0.0;
    for (int i = 0; i < 513; i++) {
        energy += (double)fabsf(P_hat[i]);
    }
    free(P_hat);
    return (float)energy;
}

void freqSmooth(void *handle, int fs, int index)
{
    (void)fs;
    pHRTF *para = (pHRTF *)handle;
    float *x = &para->absH[index][0];
    arrayMag2db(x);
    meloctsmooth(x, para->Hsm, index);
}

float singleChannelEnergy(void *handle, float *wav)
{
    pHRTF *phrtfPara = (pHRTF *)handle;
    float *audioData = (float *)malloc_memset(768000); /* 0xbb800 bytes = 192000 floats */

    static const float biquadCoeff[5] = {
        0.53095657f, 0.4267247f, 0.0f, -0.042318717f, 0.0f
    };
    float z[2] = { 0.0f, 0.0f };

    if (phrtfPara->blockNum1 < 92) {
        phrtfPara->fconvHandle1 = (fconvStruct *)fast_conv_upols_init(2048, phrtfPara->h1, 9600);
        for (int i = 0; i < phrtfPara->blockNum1; i++) {
            fast_conv_upols(phrtfPara->fconvHandle1, wav + i * 2048, audioData + i * 2048);
        }

        int peak = peak_detection(audioData, 96000, 0);
        if (peak > 4096 && peak < 91201) {
            float energySum = 0.0f;
            for (int i = 0; i < 8192; i++) {
                energySum += (float)abs((int)audioData[peak - 4096 + i]);
            }
            float energyAvg = energySum * (1.0f / 8192.0f);

            if (energyAvg <= 0.0f || audioData[peak] * 0.1f <= energyAvg) {
                fast_conv_upols_free(phrtfPara->fconvHandle1);
                free(audioData);
                return 0.0f;
            }

            biquad_df2t((float *)biquadCoeff, z, wav + peak, 96000);
            memset(audioData, 0, 384000); /* 0x5dc00 bytes = 96000 floats */

            float energy = 0.0f;
            for (int i = 0; i < 96000; i++) {
                float sample = wav[peak + i];
                energy += sample * sample;
            }

            fast_conv_upols_free(phrtfPara->fconvHandle1);
            free(audioData);

            if (energy <= 500.0f) {
                return energy;
            }
            return 0.0f;
        }
        fast_conv_upols_free(phrtfPara->fconvHandle1);
        free(audioData);
    }
    return 0.0f;
}

uint8_t hrtfWavCheck(void *handle, float *wav45pL, float *wav45pR, float *wav0L, float *wav0R,
                     float *wav45nL, float *wav45nR)
{
    float *wav[6];
    for (int i = 0; i < 6; i++) {
        wav[i] = (float *)malloc(800000);
        memset(wav[i] + 187200, 0, 51200); /* 12800 floats */
    }
    memcpy(wav[0], wav45pL, 748800); /* 187200 floats */
    memcpy(wav[1], wav45pR, 748800);
    memcpy(wav[2], wav0L, 748800);
    memcpy(wav[3], wav0R, 748800);
    memcpy(wav[4], wav45nL, 748800);
    memcpy(wav[5], wav45nR, 748800);

    float e45pL = singleChannelEnergy(handle, wav[0]);
    float e45pR = singleChannelEnergy(handle, wav[1]);
    float e0L   = singleChannelEnergy(handle, wav[2]);
    float e0R   = singleChannelEnergy(handle, wav[3]);
    float e45nL = singleChannelEnergy(handle, wav[4]);
    float e45nR = singleChannelEnergy(handle, wav[5]);

    uint8_t ret = 0;

    /* Left ear receives more energy at +45 degrees */
    if (e45pL > e45pR && e45pL != 0.0f && e45pR != 0.0f) {
        ret = 100;
    }

    /* Balanced energy between ears at 0 degrees */
    if (e0L != 0.0f && e0R != 0.0f) {
        double ratio = (double)e0L / (double)e0R;
        if (ratio > 0.3 && ratio < 3.3) {
            ret |= 10;
        }
    }

    /* Right ear receives more energy at -45 degrees */
    if (e45nR > e45nL && e45nR != 0.0f && e45nL != 0.0f) {
        ret += 1;
    }

    for (int i = 0; i < 6; i++) {
        free(wav[i]);
    }
    return ret;
}

int phrtf_process(void *handle, float *x00, float *x45p, float *x45n)
{
    pHRTF *phrtfPara = (pHRTF *)handle;

    float *pSrc_45p = (float *)malloc(800000);
    memset(pSrc_45p + 187200, 0, 51200);
    memcpy(pSrc_45p, x45p, 748800);

    float *pSrc_00 = (float *)malloc(800000);
    memset(pSrc_00 + 187200, 0, 51200);
    memcpy(pSrc_00, x00, 748800);

    float *pSrc_45n = (float *)malloc(800000);
    memset(pSrc_45n + 187200, 0, 51200);
    memcpy(pSrc_45n, x45n, 748800);

    float *pDst_00  = (float *)malloc_memset(768000);
    float *pDst_45p = (float *)malloc_memset(768000);
    float *pDst_45n = (float *)malloc_memset(768000);

    single_channel_process(handle, pSrc_00, pDst_00);
    single_channel_process(handle, pSrc_45p, pDst_45p);
    single_channel_process(handle, pSrc_45n, pDst_45n);

    energyMatch(handle, pDst_00, 0);
    arrayMag2db(&phrtfPara->absH[0][0]);
    meloctsmooth(&phrtfPara->absH[0][0], phrtfPara->Hsm, 0);

    energyMatch(handle, pDst_45p, 1);
    arrayMag2db(&phrtfPara->absH[1][0]);
    meloctsmooth(&phrtfPara->absH[1][0], phrtfPara->Hsm, 1);

    energyMatch(handle, pDst_45n, 2);
    arrayMag2db(&phrtfPara->absH[2][0]);
    meloctsmooth(&phrtfPara->absH[2][0], phrtfPara->Hsm, 2);

    int indexStamp = paraMatch(phrtfPara->Hsm);

    free(pDst_00);
    free(pDst_45p);
    free(pDst_45n);
    free(pSrc_00);
    free(pSrc_45p);
    free(pSrc_45n);

    return indexStamp;
}

void phrtf_free(void *handle)
{
    pHRTF *para = (pHRTF *)handle;
    if (!para) return;

    safe_release(para->x);
    safe_release(para->hannWin);
    safe_release(para->hDstEnergy);
    safe_release(para->h1);
    safe_release(para->h2);
    free(para->fft);
    free(para->ifft);
    free(para->X);
    safe_release(para->Hsm);
    safe_release(para);
}

int32_t **allocateAndFill2DArray(int rows, int cols, int indexStamp, int fs, int *inputShift)
{
    int32_t **twoDArray = (int32_t **)malloc((size_t)rows * sizeof(int32_t *));
    if (rows > 0) {
        for (int i = 0; i < rows; i++) {
            twoDArray[i] = (int32_t *)malloc((size_t)cols * sizeof(int32_t));
        }
    }
    convert1Dto2D(indexStamp, twoDArray, 1800, fs, rows, cols);
    if (fs == 44100) {
        *inputShift = inputShift44k[indexStamp];
    } else if (fs == 48000) {
        *inputShift = inputShift48k[indexStamp];
    }
    return twoDArray;
}

void freeAndFill2DArray(int32_t **ptr, int rows)
{
    if (rows > 0 && ptr) {
        for (int i = 0; i < rows; i++) {
            if (ptr[i]) free(ptr[i]);
        }
    }
    if (ptr) free(ptr);
}

#ifdef __cplusplus
}
#endif
