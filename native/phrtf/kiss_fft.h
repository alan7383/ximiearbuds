#ifndef KISS_FFT_H
#define KISS_FFT_H

#include <stdlib.h>
#include <stdio.h>
#include <math.h>
#include <string.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifndef kiss_fft_scalar
#define kiss_fft_scalar float
#endif

typedef struct {
    kiss_fft_scalar r;
    kiss_fft_scalar i;
} kiss_fft_cpx;

#define MAXFACTORS 32

struct kiss_fft_state {
    int nfft;
    int inverse;
    int factors[2 * MAXFACTORS];
    kiss_fft_cpx twiddles[1];
};

typedef struct kiss_fft_state* kiss_fft_cfg;

kiss_fft_cfg kiss_fft_alloc(int nfft, int inverse_fft, void *mem, size_t *lenmem);
void kiss_fft_stride(kiss_fft_cfg cfg, const kiss_fft_cpx *fin, kiss_fft_cpx *fout, int fin_stride);
void kiss_fft(kiss_fft_cfg cfg, const kiss_fft_cpx *fin, kiss_fft_cpx *fout);
void kiss_fft_cleanup(void);
int kiss_fft_next_fast_size(int n);

#ifdef __cplusplus
}
#endif

#endif /* KISS_FFT_H */
