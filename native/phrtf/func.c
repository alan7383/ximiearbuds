#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <math.h>
#include "kiss_fftr.h"

#ifdef __cplusplus
extern "C" {
#endif

/* Forward declarations */
float db2mag(float db);

typedef struct _fconvStruct fconvStruct;

typedef struct pHRTF {
    float *hannWin;
    float *hDstEnergy;
    float absH[3][2048];
    float *Hsm;
    kiss_fftr_cfg fft;
    kiss_fftr_cfg ifft;
    kiss_fft_cpx *X;
    float *x;
    int blockNum1;
    int blockNum2;
    int fs;
    float *h1;
    float *h2;
    fconvStruct *fconvHandle1;
    fconvStruct *fconvHandle2;
} pHRTF;

extern const int32_t sos44[17][1800];
extern const int32_t sos48[17][1800];

static const int32_t Hstr44[17][15] = {
    {-4, -1, 16, 0, -9, -3, -2, 19, 4, -17, 6, 1, 14, -6, -15},
    {3, -1, 13, -10, -4, -1, -5, 15, 3, -12, 13, 3, 8, -14, -11},
    {2, 2, 14, -4, -15, 0, -3, 14, -2, -6, 15, 2, 11, -11, -18},
    {-6, -2, 10, 2, -4, -8, -6, 14, 1, -2, 6, 1, 13, -1, -20},
    {0, 0, 10, -6, -4, -5, -5, 11, 3, -3, 15, 3, 7, -10, -14},
    {0, -1, 16, -4, -9, 0, -1, 20, -4, -15, 9, 6, 19, -8, -26},
    {0, 0, 11, -10, 0, 0, 0, 13, -2, -10, 12, 4, 11, -19, -9},
    {-8, -1, 10, -1, 1, -6, -5, 12, 2, -3, 6, 3, 8, -4, -13},
    {-6, 3, 15, -10, 0, -8, -2, 15, 1, -6, 8, 2, 9, -1, -19},
    {-8, 3, 13, -7, 0, -5, -1, 12, 1, -6, 8, 4, 10, -14, -9},
    {-1, -2, 15, 1, -12, 0, -1, 14, 3, -15, 8, 2, 14, -5, -20},
    {3, 5, 18, -14, -13, 1, 0, 18, -8, -12, 14, 8, 18, -11, -29},
    {-1, -6, 14, -5, -1, -5, -6, 12, 0, -1, 8, -1, 12, 0, -18},
    {-7, -3, 14, -7, 3, -6, -4, 13, 7, -9, 1, 0, 11, -7, -5},
    {-8, -2, 10, -3, 4, -8, -5, 11, 8, -5, 3, 0, 11, -5, -8},
    {-6, -4, 13, -2, 0, -2, 0, 19, 0, -16, 2, 2, 14, -3, -15},
    {0, 9, 12, -23, 1, -2, 5, 11, -13, -1, 12, 10, 13, -29, -6}
};

void winHamming(int winLen, float *win)
{
    if (winLen > 0) {
        float winsum = 0.0f;
        for (int i = 0; i < winLen; i++) {
            double c = cos(((double)i * 6.283185307179586) / (double)(winLen - 1));
            float val = (float)(0.54 - 0.46 * c);
            win[i] = val;
            winsum += val;
        }
        if (winsum != 0.0f) {
            float inv = 1.0f / winsum;
            for (int i = 0; i < winLen; i++) {
                win[i] *= inv;
            }
        }
    }
}

float vectorMul(float *x, float *y, int vectorSize)
{
    if (vectorSize < 1) return 0.0f;
    float sum = 0.0f;
    for (int i = 0; i < vectorSize; i++) {
        sum += x[i] * y[i];
    }
    return sum;
}

void meloctsmooth(float *H, float *Hsm, int index)
{
    int base = index * 5;

    /* Band 0: 11-point normalized Hamming window over H[8..18] */
    static const float win11[11] = {
        0.014598538f, 0.030629957f, 0.07260076f, 0.12447952f, 0.16645032f,
        0.18248174f,
        0.16645032f, 0.12447952f, 0.07260076f, 0.030629957f, 0.014598538f
    };
    float sum0 = 0.0f;
    for (int i = 0; i < 11; i++) {
        sum0 += H[8 + i] * win11[i];
    }
    Hsm[base + 0] = sum0;

    /* Band 1: 26-point Hamming window over H[23..48] */
    float win26[26];
    float sumWin26 = 0.0f;
    for (int i = 0; i < 26; i++) {
        double c = cos(((double)i * 6.283185307179586) / 25.0);
        win26[i] = (float)(0.54 - 0.46 * c);
        sumWin26 += win26[i];
    }
    float sum1 = 0.0f;
    for (int i = 0; i < 26; i++) {
        sum1 += H[23 + i] * (win26[i] / sumWin26);
    }
    Hsm[base + 1] = sum1;

    /* Band 2: 52-point Hamming window over H[49..100] */
    float win52[52];
    float sumWin52 = 0.0f;
    for (int i = 0; i < 52; i++) {
        double c = cos(((double)i * 6.283185307179586) / 51.0);
        win52[i] = (float)(0.54 - 0.46 * c);
        sumWin52 += win52[i];
    }
    float sum2 = 0.0f;
    for (int i = 0; i < 52; i++) {
        sum2 += H[49 + i] * (win52[i] / sumWin52);
    }
    Hsm[base + 2] = sum2;

    /* Band 3: 100-point Hamming window over H[96..195] */
    float win100[100];
    float sumWin100 = 0.0f;
    for (int i = 0; i < 100; i++) {
        double c = cos(((double)i * 6.283185307179586) / 99.0);
        win100[i] = (float)(0.54 - 0.46 * c);
        sumWin100 += win100[i];
    }
    float sum3 = 0.0f;
    for (int i = 0; i < 100; i++) {
        sum3 += H[96 + i] * (win100[i] / sumWin100);
    }
    Hsm[base + 3] = sum3;

    /* Band 4: 184-point Hamming window over H[180..363] */
    float win184[184];
    float sumWin184 = 0.0f;
    for (int i = 0; i < 184; i++) {
        double c = cos(((double)i * 6.283185307179586) / 183.0);
        win184[i] = (float)(0.54 - 0.46 * c);
        sumWin184 += win184[i];
    }
    float sum4 = 0.0f;
    for (int i = 0; i < 184; i++) {
        sum4 += H[180 + i] * (win184[i] / sumWin184);
    }
    Hsm[base + 4] = sum4;

    /* Normalize: subtract the mean of bands 1..4 */
    float avg = (Hsm[base + 1] + Hsm[base + 2] + Hsm[base + 3] + Hsm[base + 4]) * 0.25f;
    for (int i = 0; i < 5; i++) {
        Hsm[base + i] -= avg;
    }
}

void arrayMag2db(float *x)
{
    for (int i = 0; i < 2048; i++) {
        x[i] = (float)((log((double)x[i]) / 2.302585092994046) * 20.0);
    }
}

float calculateMSE(float *actual, float *subject)
{
    return (actual[1] - subject[1]) * (actual[1] - subject[1]) +
           (actual[2] - subject[2]) * (actual[2] - subject[2]) +
           (actual[3] - subject[3]) * (actual[3] - subject[3]) +
           (actual[4] - subject[4]) * (actual[4] - subject[4]) +
           (actual[6] - subject[6]) * (actual[6] - subject[6]) +
           (actual[7] - subject[7]) * (actual[7] - subject[7]) +
           (actual[8] - subject[8]) * (actual[8] - subject[8]) +
           (actual[9] - subject[9]) * (actual[9] - subject[9]) +
           (actual[11] - subject[11]) * (actual[11] - subject[11]) +
           (actual[12] - subject[12]) * (actual[12] - subject[12]) +
           (actual[13] - subject[13]) * (actual[13] - subject[13]) +
           (actual[14] - subject[14]) * (actual[14] - subject[14]);
}

int paraMatch(float *actual)
{
    int bestIndex = 0;
    float minError = 1e30f;

    for (int m = 0; m < 17; m++) {
        const int32_t *p = Hstr44[m];
        float error = (actual[1] - (float)p[1]) * (actual[1] - (float)p[1]) +
                      (actual[2] - (float)p[2]) * (actual[2] - (float)p[2]) +
                      (actual[3] - (float)p[3]) * (actual[3] - (float)p[3]) +
                      (actual[4] - (float)p[4]) * (actual[4] - (float)p[4]) +
                      (actual[6] - (float)p[6]) * (actual[6] - (float)p[6]) +
                      (actual[7] - (float)p[7]) * (actual[7] - (float)p[7]) +
                      (actual[8] - (float)p[8]) * (actual[8] - (float)p[8]) +
                      (actual[9] - (float)p[9]) * (actual[9] - (float)p[9]) +
                      (actual[11] - (float)p[11]) * (actual[11] - (float)p[11]) +
                      (actual[12] - (float)p[12]) * (actual[12] - (float)p[12]) +
                      (actual[13] - (float)p[13]) * (actual[13] - (float)p[13]) +
                      (actual[14] - (float)p[14]) * (actual[14] - (float)p[14]);

        if (error < minError) {
            minError = error;
            bestIndex = m;
        }
    }
    return bestIndex;
}

void convert1Dto2D(int indexStamp, int32_t **twoDArray, int oneDSize, int fs, int rows, int cols)
{
    if (cols * rows == oneDSize) {
        const int32_t *src = NULL;
        if (fs == 44100) {
            src = sos44[indexStamp];
        } else if (fs == 48000) {
            src = sos48[indexStamp];
        }
        if (src && rows > 0 && cols > 0) {
            for (int i = 0; i < rows; i++) {
                memcpy(twoDArray[i], src + i * cols, (size_t)cols * sizeof(int32_t));
            }
        }
    } else {
        puts("Size mismatch between 1D and 2D array specifications.");
        *twoDArray = NULL;
    }
}

void safe_release(void *pt)
{
    free(pt);
}

int peak_detection(float *audioData, int length, int sweeptype)
{
    (void)sweeptype;
    if (length < 1) return -1;
    float max1 = -2.1474836e+09f;
    float max2 = -2.1474836e+09f;
    int max1Index = -1;
    int max2Index = -1;

    for (int i = 0; i < length; i++) {
        float sample = audioData[i];
        if (sample > max1) {
            max2 = max1;
            max2Index = max1Index;
            max1 = sample;
            max1Index = i;
        } else if (sample > max2) {
            max2 = sample;
            max2Index = i;
        }
    }
    if (max1 <= max2) {
        return max2Index;
    }
    return max1Index;
}

void *malloc_memset(int size)
{
    void *pt = malloc((size_t)size);
    if (pt) {
        memset(pt, 0, (size_t)size);
    }
    return pt;
}

void gethrtf(int indexStamp, int32_t hrtfData44k)
{
    (void)indexStamp;
    (void)hrtfData44k;
}

void hannWindow(float *window, int length)
{
    if (length > 0) {
        double dN = (double)(length + 1);
        for (int i = 0; i < length; i++) {
            double c = cos(((double)(i + 1) * 6.283185307179586) / dN);
            window[i] = (float)((1.0 - c) * 0.5);
        }
    }
}

void invlogSweep(float *sweep, int T, float f1, float f2, float duration, int fs)
{
    double fRatio = (double)((f2 * 6.2831855f) / (f1 * 6.2831855f));
    double log_ratio = log(fRatio);
    double octaves = log2(fRatio);

    int fadeLen = (int)((double)fs * 0.1);
    int winTotal = fadeLen * 2;
    float *win = (float *)malloc((size_t)winTotal * sizeof(float));
    if (fadeLen > 0 && win) {
        double dN = (double)(winTotal | 1);
        for (int i = 0; i < winTotal; i++) {
            double c = cos(((double)(i + 1) * 6.283185307179586) / dN);
            win[i] = (float)((1.0 - c) * 0.5);
        }
    }

    if (T > 0) {
        for (int i = 0; i < T; i++) {
            int inv_i = T - 1 - i;
            double t = (double)i / (double)fs;
            double exp_val = exp((t / (double)duration) * (double)(float)log_ratio);
            double phase = ((double)(f1 * 6.2831855f * duration) / (double)(float)log_ratio) * (exp_val - 1.0);
            float amp_db = ((float)(octaves * -6.0) / (float)(T - 1)) * (float)inv_i;
            float mag = db2mag(amp_db);
            sweep[inv_i] = (float)(sin(phase) * 0.251188 * (double)mag * 0.04019999876618385);
        }

        if (fadeLen > 0 && win) {
            int fadeCount = fadeLen;
            if (fadeCount > T) fadeCount = T;
            for (int i = 0; i < fadeCount; i++) {
                sweep[i] *= win[i];
            }
            for (int i = 0; i < fadeCount; i++) {
                sweep[T - fadeCount + i] *= win[winTotal - fadeCount + i];
            }
        }
    }
    if (win) free(win);
}

void invlinearSweep(float *sweep, int fstart, int fend, float duration, int fs)
{
    float ffs = (float)fs;
    int len = (int)(ffs * duration);
    if (len > 0) {
        float f0 = (float)fstart;
        double k = ((double)(fend - fstart) / duration) * 0.5;
        for (int i = 0; i < len; i++) {
            double t = (double)i / (double)ffs;
            sweep[i] = (float)sin((t * (double)f0 + t * t * k) * 6.283185307179586);
        }

        int fadeLen = (int)(ffs * 0.03f);
        if (fadeLen > 0) {
            int winTotal = fadeLen * 2;
            float *win = (float *)malloc((size_t)winTotal * sizeof(float));
            if (win) {
                double dN = (double)(winTotal | 1);
                for (int i = 0; i < winTotal; i++) {
                    double c = cos(((double)(i + 1) * 6.283185307179586) / dN);
                    win[i] = (float)((1.0 - c) * 0.5);
                }
                for (int i = 0; i < fadeLen; i++) {
                    sweep[i] *= win[i];
                }
                for (int i = 0; i < fadeLen; i++) {
                    sweep[len - fadeLen + i] *= win[fadeLen + i];
                }
                free(win);
            }
        }

        for (int i = 0; i < len / 2; i++) {
            float tmp = sweep[i];
            sweep[i] = sweep[len - 1 - i];
            sweep[len - 1 - i] = tmp;
        }
    }
}

void mypwelch(void *handle, float *x, int N, int L, int D, float *P_hat)
{
    pHRTF *para = (pHRTF *)handle;
    int K = 0;
    if (D != 0) {
        K = (N - L) / D;
    }
    int numBins = L / 2;
    memset(P_hat, 0, (size_t)(numBins + 1) * sizeof(float));

    float *window = (float *)malloc((size_t)L * sizeof(float));
    float hannEnergy = 0.0f;
    if (L > 0) {
        double dN = (double)(L + 1);
        for (int i = 0; i < L; i++) {
            double c = cos(((double)(i + 1) * 6.283185307179586) / dN);
            window[i] = (float)((1.0 - c) * 0.5);
            hannEnergy += window[i] * window[i];
        }
    }
    float scale = (float)L / hannEnergy;

    float *xk = (float *)malloc((size_t)L * sizeof(float));
    for (int k = 0; k <= K; k++) {
        for (int i = 0; i < L; i++) {
            xk[i] = x[k * D + i] * window[i];
        }
        kiss_fftr(para->fft, xk, para->X);
        float invL = 1.0f / (float)L;
        for (int i = 0; i <= numBins; i++) {
            para->X[i].r *= invL;
            para->X[i].i *= invL;
        }
        for (int i = 0; i <= numBins; i++) {
            float r = para->X[i].r;
            float im = para->X[i].i;
            float val = fabsf(r * r - im * im + 2.0f * r * im);
            P_hat[i] += (float)((double)val * (double)scale);
        }
    }
    free(xk);
    free(window);

    float numSegs = (float)(K + 1);
    for (int i = 0; i <= numBins; i++) {
        P_hat[i] /= numSegs;
    }
}

void biquad_df2t(float *coeff, float *z, float *data, uint32_t dataLen)
{
    if (dataLen == 0) return;
    float b0 = coeff[0];
    float b1 = coeff[1];
    float b2 = coeff[2];
    float a1 = coeff[3];
    float a2 = coeff[4];
    float z0 = z[0];
    float z1 = z[1];

    for (uint32_t i = 0; i < dataLen; i++) {
        float in = data[i];
        float out = z0 + in * b0;
        data[i] = out;
        z0 = z1 + in * b1 - out * a1;
        z1 = in * b2 - out * a2;
    }
    z[0] = z0;
    z[1] = z1;
}

#ifdef __cplusplus
}
#endif
