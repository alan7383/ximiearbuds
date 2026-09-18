#ifndef PHRTF_H
#define PHRTF_H

#include <stdint.h>
#include <stdlib.h>
#include "kiss_fftr.h"
#include "fast_conv.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct pHRTF {
    float *hannWin;             /* 0x00: 2048-float Hann window */
    float *hDstEnergy;          /* 0x08: 3-float target energies */
    float absH[3][2048];        /* 0x10: 3 angles x 2048 PSD bins */
    float *Hsm;                 /* 0x6010: 15-float smoothed bands (3x5) */
    kiss_fftr_cfg fft;          /* 0x6018: 1024-point real FFT */
    kiss_fftr_cfg ifft;         /* 0x6020: 1024-point real IFFT */
    kiss_fft_cpx *X;            /* 0x6028: 1025 complex buffer */
    float *x;                   /* 0x6030: 2048 float buffer */
    int blockNum1;              /* 0x6038: 60 blocks for linear sweep */
    int blockNum2;              /* 0x603c: 47 blocks for log sweep */
    int fs;                     /* 0x6040: sample rate (48000) */
    float *h1;                  /* 0x6048: inverted linear sweep (9600 floats) */
    float *h2;                  /* 0x6050: inverted log sweep (96000 floats) */
    fconvStruct *fconvHandle1;  /* 0x6058: UPOLS convolution handle 1 */
    fconvStruct *fconvHandle2;  /* 0x6060: UPOLS convolution handle 2 */
} pHRTF;

void *phrtf_init(void);
int phrtf_process(void *handle, float *x00, float *x45p, float *x45n);
void phrtf_free(void *handle);

float hEnergy(void *handle, float *h, int index);
void energyMatch(void *handle, float *hOri, int angleIndex);
void single_channel_process(void *handle, float *pSrc, float *pDst);
float singleChannelEnergy(void *handle, float *wav);
uint8_t hrtfWavCheck(void *handle, float *wav45pL, float *wav45pR, float *wav0L, float *wav0R,
                     float *wav45nL, float *wav45nR);
void freqSmooth(void *handle, int fs, int index);

int32_t **allocateAndFill2DArray(int rows, int cols, int indexStamp, int fs, int *inputShift);
void freeAndFill2DArray(int32_t **ptr, int rows);

#ifdef __cplusplus
}
#endif

#endif /* PHRTF_H */
