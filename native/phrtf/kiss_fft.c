#include "kiss_fft.h"

static void kf_work(kiss_fft_cpx *Fout, const kiss_fft_cpx *f, size_t fstride, int in_stride, int *factors, const kiss_fft_cfg st)
{
    kiss_fft_cpx *Fout_beg = Fout;
    const int p = *factors++;
    const int m = *factors++;
    const kiss_fft_cpx *Fout_end = Fout + p * m;

    if (m == 1) {
        do {
            *Fout = *f;
            f += fstride * in_stride;
        } while (++Fout != Fout_end);
    } else {
        do {
            kf_work(Fout, f, fstride * p, in_stride, factors, st);
            f += fstride * in_stride;
        } while ((Fout += m) != Fout_end);
    }

    Fout = Fout_beg;

    switch (p) {
    case 2: {
        const kiss_fft_cpx *tw1 = st->twiddles;
        for (int k = 0; k < m; ++k) {
            kiss_fft_cpx t;
            t.r = Fout[m].r * tw1->r - Fout[m].i * tw1->i;
            t.i = Fout[m].r * tw1->i + Fout[m].i * tw1->r;
            tw1 += fstride;
            Fout[m].r = Fout->r - t.r;
            Fout[m].i = Fout->i - t.i;
            Fout->r += t.r;
            Fout->i += t.i;
            ++Fout;
        }
        break;
    }
    case 3: {
        size_t k = m;
        const size_t m2 = 2 * m;
        const kiss_fft_cpx *tw1 = st->twiddles;
        const kiss_fft_cpx *tw2 = st->twiddles;
        const float epi3_i = st->twiddles[fstride * m].i;

        do {
            kiss_fft_cpx scratch[5];
            scratch[1].r = Fout[m].r * tw1->r - Fout[m].i * tw1->i;
            scratch[1].i = Fout[m].r * tw1->i + Fout[m].i * tw1->r;
            scratch[2].r = Fout[m2].r * tw2->r - Fout[m2].i * tw2->i;
            scratch[2].i = Fout[m2].r * tw2->i + Fout[m2].i * tw2->r;

            scratch[3].r = scratch[1].r + scratch[2].r;
            scratch[3].i = scratch[1].i + scratch[2].i;
            scratch[0].r = scratch[1].r - scratch[2].r;
            scratch[0].i = scratch[1].i - scratch[2].i;
            tw1 += fstride;
            tw2 += fstride * 2;

            Fout[m].r = Fout->r - scratch[3].r * 0.5f;
            Fout[m].i = Fout->i - scratch[3].i * 0.5f;

            scratch[0].r *= epi3_i;
            scratch[0].i *= epi3_i;

            Fout->r += scratch[3].r;
            Fout->i += scratch[3].i;

            Fout[m2].r = Fout[m].r + scratch[0].i;
            Fout[m2].i = Fout[m].i - scratch[0].r;

            Fout[m].r -= scratch[0].i;
            Fout[m].i += scratch[0].r;

            ++Fout;
        } while (--k);
        break;
    }
    case 4: {
        const kiss_fft_cpx *tw1 = st->twiddles;
        const kiss_fft_cpx *tw2 = st->twiddles;
        const kiss_fft_cpx *tw3 = st->twiddles;
        size_t k = m;
        const size_t m2 = 2 * m;
        const size_t m3 = 3 * m;

        do {
            kiss_fft_cpx scratch[6];
            scratch[0].r = Fout[m].r * tw1->r - Fout[m].i * tw1->i;
            scratch[0].i = Fout[m].r * tw1->i + Fout[m].i * tw1->r;
            scratch[1].r = Fout[m2].r * tw2->r - Fout[m2].i * tw2->i;
            scratch[1].i = Fout[m2].r * tw2->i + Fout[m2].i * tw2->r;
            scratch[2].r = Fout[m3].r * tw3->r - Fout[m3].i * tw3->i;
            scratch[2].i = Fout[m3].r * tw3->i + Fout[m3].i * tw3->r;

            scratch[5].r = Fout->r - scratch[1].r;
            scratch[5].i = Fout->i - scratch[1].i;
            Fout->r += scratch[1].r;
            Fout->i += scratch[1].i;
            scratch[3].r = scratch[0].r + scratch[2].r;
            scratch[3].i = scratch[0].i + scratch[2].i;
            scratch[4].r = scratch[0].r - scratch[2].r;
            scratch[4].i = scratch[0].i - scratch[2].i;

            Fout[m2].r = Fout->r - scratch[3].r;
            Fout[m2].i = Fout->i - scratch[3].i;
            tw1 += fstride;
            tw2 += fstride * 2;
            tw3 += fstride * 3;
            Fout->r += scratch[3].r;
            Fout->i += scratch[3].i;

            if (st->inverse) {
                Fout[m].r = scratch[5].r - scratch[4].i;
                Fout[m].i = scratch[5].i + scratch[4].r;
                Fout[m3].r = scratch[5].r + scratch[4].i;
                Fout[m3].i = scratch[5].i - scratch[4].r;
            } else {
                Fout[m].r = scratch[5].r + scratch[4].i;
                Fout[m].i = scratch[5].i - scratch[4].r;
                Fout[m3].r = scratch[5].r - scratch[4].i;
                Fout[m3].i = scratch[5].i + scratch[4].r;
            }
            ++Fout;
        } while (--k);
        break;
    }
    case 5: {
        kiss_fft_cpx ya = st->twiddles[fstride * m];
        kiss_fft_cpx yb = st->twiddles[fstride * 2 * m];
        const kiss_fft_cpx *tw = st->twiddles;
        kiss_fft_cpx *Fout0 = Fout;
        kiss_fft_cpx *Fout1 = Fout0 + m;
        kiss_fft_cpx *Fout2 = Fout0 + 2 * m;
        kiss_fft_cpx *Fout3 = Fout0 + 3 * m;
        kiss_fft_cpx *Fout4 = Fout0 + 4 * m;

        for (int u = 0; u < m; ++u) {
            kiss_fft_cpx scratch[13];
            scratch[0] = *Fout0;

            scratch[1].r = Fout1->r * tw[u * fstride].r - Fout1->i * tw[u * fstride].i;
            scratch[1].i = Fout1->r * tw[u * fstride].i + Fout1->i * tw[u * fstride].r;
            scratch[2].r = Fout2->r * tw[2 * u * fstride].r - Fout2->i * tw[2 * u * fstride].i;
            scratch[2].i = Fout2->r * tw[2 * u * fstride].i + Fout2->i * tw[2 * u * fstride].r;
            scratch[3].r = Fout3->r * tw[3 * u * fstride].r - Fout3->i * tw[3 * u * fstride].i;
            scratch[3].i = Fout3->r * tw[3 * u * fstride].i + Fout3->i * tw[3 * u * fstride].r;
            scratch[4].r = Fout4->r * tw[4 * u * fstride].r - Fout4->i * tw[4 * u * fstride].i;
            scratch[4].i = Fout4->r * tw[4 * u * fstride].i + Fout4->i * tw[4 * u * fstride].r;

            scratch[7].r = scratch[1].r + scratch[4].r;
            scratch[7].i = scratch[1].i + scratch[4].i;
            scratch[10].r = scratch[1].r - scratch[4].r;
            scratch[10].i = scratch[1].i - scratch[4].i;
            scratch[8].r = scratch[2].r + scratch[3].r;
            scratch[8].i = scratch[2].i + scratch[3].i;
            scratch[9].r = scratch[2].r - scratch[3].r;
            scratch[9].i = scratch[2].i - scratch[3].i;

            Fout0->r += scratch[7].r + scratch[8].r;
            Fout0->i += scratch[7].i + scratch[8].i;

            scratch[5].r = scratch[0].r + scratch[7].r * ya.r + scratch[8].r * yb.r;
            scratch[5].i = scratch[0].i + scratch[7].i * ya.r + scratch[8].i * yb.r;

            scratch[6].r = scratch[10].i * ya.i + scratch[9].i * yb.i;
            scratch[6].i = -scratch[10].r * ya.i - scratch[9].r * yb.i;

            Fout1->r = scratch[5].r - scratch[6].r;
            Fout1->i = scratch[5].i - scratch[6].i;
            Fout4->r = scratch[5].r + scratch[6].r;
            Fout4->i = scratch[5].i + scratch[6].i;

            scratch[11].r = scratch[0].r + scratch[7].r * yb.r + scratch[8].r * ya.r;
            scratch[11].i = scratch[0].i + scratch[7].i * yb.r + scratch[8].i * ya.r;
            scratch[12].r = -scratch[10].i * yb.i + scratch[9].i * ya.i;
            scratch[12].i = scratch[10].r * yb.i - scratch[9].r * ya.i;

            Fout2->r = scratch[11].r + scratch[12].r;
            Fout2->i = scratch[11].i + scratch[12].i;
            Fout3->r = scratch[11].r - scratch[12].r;
            Fout3->i = scratch[11].i - scratch[12].i;

            ++Fout0; ++Fout1; ++Fout2; ++Fout3; ++Fout4;
        }
        break;
    }
    default: {
        kiss_fft_cpx *scratch = (kiss_fft_cpx *)malloc(sizeof(kiss_fft_cpx) * p);
        if (!scratch) {
            printf("Memory allocation failed.");
            return;
        }
        for (int u = 0; u < m; ++u) {
            int k = u;
            for (int q1 = 0; q1 < p; ++q1) {
                scratch[q1] = Fout_beg[k];
                k += m;
            }
            k = u;
            for (int q1 = 0; q1 < p; ++q1) {
                int twidx = 0;
                Fout_beg[k] = scratch[0];
                for (int q = 1; q < p; ++q) {
                    twidx += (int)(k * fstride);
                    if (twidx >= st->nfft) twidx -= st->nfft;
                    kiss_fft_cpx t;
                    t.r = scratch[q].r * st->twiddles[twidx].r - scratch[q].i * st->twiddles[twidx].i;
                    t.i = scratch[q].r * st->twiddles[twidx].i + scratch[q].i * st->twiddles[twidx].r;
                    Fout_beg[k].r += t.r;
                    Fout_beg[k].i += t.i;
                }
                k += m;
            }
        }
        free(scratch);
        break;
    }
    }
}

kiss_fft_cfg kiss_fft_alloc(int nfft, int inverse_fft, void *mem, size_t *lenmem)
{
    size_t memneeded = sizeof(struct kiss_fft_state) + sizeof(kiss_fft_cpx) * (nfft - 1);
    kiss_fft_cfg st = NULL;

    if (lenmem == NULL) {
        st = (kiss_fft_cfg)malloc(memneeded);
    } else {
        if (mem != NULL && *lenmem >= memneeded)
            st = (kiss_fft_cfg)mem;
        *lenmem = memneeded;
    }

    if (st) {
        st->nfft = nfft;
        st->inverse = inverse_fft;

        for (int i = 0; i < nfft; ++i) {
            const double pi = 3.141592653589793238462643383279502884197169399375105820974944;
            double phase = -2.0 * pi * i / nfft;
            if (st->inverse)
                phase = -phase;
            st->twiddles[i].r = (kiss_fft_scalar)cos(phase);
            st->twiddles[i].i = (kiss_fft_scalar)sin(phase);
        }

        /* Factorize */
        int *facbuf = st->factors;
        int p = 4;
        double floor_sqrt = floor(sqrt((double)nfft));
        int n = nfft;
        do {
            while (n % p) {
                switch (p) {
                case 4: p = 2; break;
                case 2: p = 3; break;
                default: p += 2; break;
                }
                if ((double)p > floor_sqrt)
                    p = n;
            }
            n /= p;
            *facbuf++ = p;
            *facbuf++ = n;
        } while (n > 1);
    }
    return st;
}

void kiss_fft_stride(kiss_fft_cfg st, const kiss_fft_cpx *fin, kiss_fft_cpx *fout, int in_stride)
{
    if (fin == fout) {
        kiss_fft_cpx *tmpbuf = (kiss_fft_cpx *)malloc(sizeof(kiss_fft_cpx) * st->nfft);
        if (!tmpbuf) {
            printf("Memory allocation error.");
            return;
        }
        kf_work(tmpbuf, fin, 1, in_stride, st->factors, st);
        memcpy(fout, tmpbuf, sizeof(kiss_fft_cpx) * st->nfft);
        free(tmpbuf);
    } else {
        kf_work(fout, fin, 1, in_stride, st->factors, st);
    }
}

void kiss_fft(kiss_fft_cfg cfg, const kiss_fft_cpx *fin, kiss_fft_cpx *fout)
{
    kiss_fft_stride(cfg, fin, fout, 1);
}

void kiss_fft_cleanup(void)
{
}

int kiss_fft_next_fast_size(int n)
{
    while (1) {
        int m = n;
        while ((m % 2) == 0) m /= 2;
        while ((m % 3) == 0) m /= 3;
        while ((m % 5) == 0) m /= 5;
        if (m <= 1)
            break;
        n++;
    }
    return n;
}
