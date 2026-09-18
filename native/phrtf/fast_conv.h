#ifndef FAST_CONV_H
#define FAST_CONV_H

#include <stdint.h>
#include <stdlib.h>
#include "kiss_fftr.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct _fconvStruct {
    uint32_t blocksize;     /* 0x00 */
    uint32_t nfft;          /* 0x04 */
    uint32_t validFFT;      /* 0x08 */
    uint32_t hBlocks;       /* 0x0c */
    float *xTemp;           /* 0x10 */
    int *FDLSidx;           /* 0x18 */
    int blockCounter;       /* 0x20 */
    int FDLStemp;           /* 0x24 */
    float *yTemp;           /* 0x28 */
    kiss_fftr_cfg fft;      /* 0x30 */
    kiss_fftr_cfg ifft;     /* 0x38 */
    kiss_fft_cpx *H;        /* 0x40 */
    kiss_fft_cpx *yTempFFT; /* 0x48 */
    kiss_fft_cpx *xFDL;     /* 0x50 */
} fconvStruct;

void *fast_conv_upols_init(uint32_t blockSize, float *h, uint32_t len);
void fast_conv_upols(void *handle, float *pSrc, float *pDst);
void fast_conv_upols_free(void *handle);

#ifdef __cplusplus
}
#endif

#endif /* FAST_CONV_H */
