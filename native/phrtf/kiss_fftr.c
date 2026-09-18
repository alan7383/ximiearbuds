#include "kiss_fftr.h"

struct kiss_fftr_state {
    kiss_fft_cfg substate;
    kiss_fft_cpx *tmpbuf;
    kiss_fft_cpx *super_twiddles;
};

kiss_fftr_cfg kiss_fftr_alloc(int nfft, int inverse_fft, void *mem, size_t *lenmem)
{
    if (nfft & 1) {
        printf("Real FFT optimization must be even.");
        return NULL;
    }
    nfft >>= 1;

    size_t subsize = 0;
    kiss_fft_alloc(nfft, inverse_fft, NULL, &subsize);
    size_t memneeded = sizeof(struct kiss_fftr_state) + subsize + sizeof(kiss_fft_cpx) * (nfft * 3 / 2);

    kiss_fftr_cfg st = NULL;
    if (lenmem == NULL) {
        st = (kiss_fftr_cfg)malloc(memneeded);
    } else {
        if (mem && *lenmem >= memneeded)
            st = (kiss_fftr_cfg)mem;
        *lenmem = memneeded;
    }

    if (!st)
        return NULL;

    st->substate = (kiss_fft_cfg)(st + 1);
    st->tmpbuf = (kiss_fft_cpx *)(((char *)st->substate) + subsize);
    st->super_twiddles = st->tmpbuf + nfft;
    kiss_fft_alloc(nfft, inverse_fft, st->substate, &subsize);

    for (int i = 0; i < nfft / 2; ++i) {
        double phase = -3.141592653589793238462643383279502884197169399375105820974944 * ((double)(i + 1) / nfft + 0.5);
        if (inverse_fft)
            phase = -phase;
        st->super_twiddles[i].r = (kiss_fft_scalar)cos(phase);
        st->super_twiddles[i].i = (kiss_fft_scalar)sin(phase);
    }
    return st;
}

void kiss_fftr(kiss_fftr_cfg st, const kiss_fft_scalar *timedata, kiss_fft_cpx *freqdata)
{
    if (st->substate->inverse) {
        printf("kiss fft usage error: improper alloc");
        return;
    }

    int ncfft = st->substate->nfft;
    kiss_fft(st->substate, (const kiss_fft_cpx *)timedata, st->tmpbuf);

    freqdata[0].r = st->tmpbuf[0].r + st->tmpbuf[0].i;
    freqdata[0].i = 0;
    freqdata[ncfft].r = st->tmpbuf[0].r - st->tmpbuf[0].i;
    freqdata[ncfft].i = 0;

    for (int k = 1; k <= ncfft / 2; ++k) {
        kiss_fft_cpx fpk = st->tmpbuf[k];
        kiss_fft_cpx fpnk;
        fpnk.r = st->tmpbuf[ncfft - k].r;
        fpnk.i = -st->tmpbuf[ncfft - k].i;

        kiss_fft_cpx f1k, f2k;
        f1k.r = fpk.r + fpnk.r;
        f1k.i = fpk.i + fpnk.i;
        f2k.r = fpk.r - fpnk.r;
        f2k.i = fpk.i - fpnk.i;

        kiss_fft_cpx tw = st->super_twiddles[k - 1];
        kiss_fft_cpx tdc;
        tdc.r = f2k.r * tw.r - f2k.i * tw.i;
        tdc.i = f2k.r * tw.i + f2k.i * tw.r;

        freqdata[k].r = (f1k.r + tdc.r) * 0.5f;
        freqdata[k].i = (f1k.i + tdc.i) * 0.5f;
        freqdata[ncfft - k].r = (f1k.r - tdc.r) * 0.5f;
        freqdata[ncfft - k].i = (tdc.i - f1k.i) * 0.5f;
    }
}

void kiss_fftri(kiss_fftr_cfg st, const kiss_fft_cpx *freqdata, kiss_fft_scalar *timedata)
{
    if (!st->substate->inverse) {
        printf("kiss fft usage error: improper alloc");
        return;
    }

    int ncfft = st->substate->nfft;
    st->tmpbuf[0].r = freqdata[0].r + freqdata[ncfft].r;
    st->tmpbuf[0].i = freqdata[0].r - freqdata[ncfft].r;

    for (int k = 1; k <= ncfft / 2; ++k) {
        kiss_fft_cpx fk = freqdata[k];
        kiss_fft_cpx fnkc;
        fnkc.r = freqdata[ncfft - k].r;
        fnkc.i = -freqdata[ncfft - k].i;

        kiss_fft_cpx fek, fok;
        fek.r = fk.r + fnkc.r;
        fek.i = fk.i + fnkc.i;
        fok.r = fk.r - fnkc.r;
        fok.i = fk.i - fnkc.i;

        kiss_fft_cpx tw = st->super_twiddles[k - 1];
        kiss_fft_cpx tmp;
        tmp.r = fok.r * tw.r - fok.i * tw.i;
        tmp.i = fok.r * tw.i + fok.i * tw.r;

        st->tmpbuf[k].r = fek.r + tmp.r;
        st->tmpbuf[k].i = fek.i + tmp.i;
        st->tmpbuf[ncfft - k].r = fek.r - tmp.r;
        st->tmpbuf[ncfft - k].i = -(fek.i - tmp.i);
    }
    kiss_fft(st->substate, st->tmpbuf, (kiss_fft_cpx *)timedata);
}
