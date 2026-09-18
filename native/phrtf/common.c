#include <stdint.h>
#include <math.h>

#ifdef __cplusplus
extern "C" {
#endif

typedef float Float_t;

uint32_t nextpow2(uint32_t v)
{
    uint32_t uVar1 = (v - 1) | ((v - 1) >> 1);
    uVar1 |= uVar1 >> 2;
    uVar1 |= uVar1 >> 4;
    uVar1 |= uVar1 >> 8;
    return (uVar1 | (uVar1 >> 16)) + 1;
}

float db2mag(float db)
{
    return (float)pow(10.0, (double)db / 20.0);
}

Float_t mag2db(Float_t mag)
{
    return log10f(mag) * 20.0f;
}

Float_t pow2db(Float_t pow)
{
    return log10f(pow) * 10.0f;
}

#ifdef __cplusplus
}
#endif
