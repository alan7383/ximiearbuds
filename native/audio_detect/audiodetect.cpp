#include <jni.h>
#include <stdint.h>
#include <stdlib.h>
#include <stdio.h>
#include <stdarg.h>
#include <string.h>

#include "phrtf.h"

#ifndef ANDROID_LOG_ERROR
#define ANDROID_LOG_ERROR 6
#endif

#ifdef __cplusplus
extern "C" {
#endif
int __android_log_print(int prio, const char *tag, const char *fmt, ...);
#ifdef __cplusplus
}
#endif

/* Global Variables */
int rows = 72;
int cols = 25;
int32_t **hrtfData44k = nullptr;
int32_t **hrtfData48k = nullptr;
int inputShift44k = 0;
int inputShift48k = 0;

/* Helper: PCM byte array to float array conversion with exact Android NDK mangling */
float* byteArray2FloatArray(JNIEnv *env, jbyteArray l1, int bitDepth) __asm__("_Z20byteArray2FloatArrayP7_JNIEnvP11_jbyteArrayi");

float* byteArray2FloatArray(JNIEnv *env, jbyteArray l1, int bitDepth)
{
    jsize byteLength = env->GetArrayLength(l1);
    jbyte *byteBuffer = env->GetByteArrayElements(l1, nullptr);
    if (!byteBuffer) return nullptr;

    int sampleCount = 0;
    switch (bitDepth) {
    case 8:
        sampleCount = byteLength;
        break;
    case 16:
        sampleCount = byteLength / 2;
        break;
    case 24:
        sampleCount = byteLength / 3;
        break;
    case 32:
        sampleCount = byteLength / 4;
        break;
    default:
        env->ReleaseByteArrayElements(l1, byteBuffer, JNI_ABORT);
        return nullptr;
    }

    float *floatBuffer = (float *)malloc(sampleCount * sizeof(float));
    if (!floatBuffer) {
        env->ReleaseByteArrayElements(l1, byteBuffer, JNI_ABORT);
        return nullptr;
    }

    for (int i = 0; i < sampleCount; i++) {
        switch (bitDepth) {
        case 8: {
            uint8_t b0 = (uint8_t)byteBuffer[i];
            int sample = (int)b0 - 128;
            floatBuffer[i] = (float)sample * (1.0f / 128.0f);
            break;
        }
        case 16: {
            int16_t sample = (int16_t)(((uint8_t)byteBuffer[2 * i]) | (((int8_t)byteBuffer[2 * i + 1]) << 8));
            floatBuffer[i] = (float)sample * (1.0f / 32768.0f);
            break;
        }
        case 24: {
            int32_t sample = ((uint8_t)byteBuffer[3 * i]) |
                             (((uint8_t)byteBuffer[3 * i + 1]) << 8) |
                             (((int8_t)byteBuffer[3 * i + 2]) << 16);
            if (sample & 0x800000) {
                sample |= 0xff000000;
            }
            floatBuffer[i] = (float)sample * (1.0f / 8388608.0f);
            break;
        }
        case 32: {
            int32_t sample = ((uint8_t)byteBuffer[4 * i]) |
                             (((uint8_t)byteBuffer[4 * i + 1]) << 8) |
                             (((uint8_t)byteBuffer[4 * i + 2]) << 16) |
                             (((int8_t)byteBuffer[4 * i + 3]) << 24);
            floatBuffer[i] = (float)sample * (1.0f / 2147483648.0f);
            break;
        }
        }
    }

    env->ReleaseByteArrayElements(l1, byteBuffer, JNI_ABORT);
    return floatBuffer;
}

extern "C" {

JNIEXPORT void JNICALL Java_com_mi_audio_phrtf_AudioDetect_nativeInit(JNIEnv *env, jobject thiz)
{
    (void)env;
    (void)thiz;
    if (hrtfData44k) {
        freeAndFill2DArray(hrtfData44k, rows);
        hrtfData44k = nullptr;
    }
    if (hrtfData48k) {
        freeAndFill2DArray(hrtfData48k, rows);
        hrtfData48k = nullptr;
    }
    inputShift44k = 0;
    inputShift48k = 0;
}

JNIEXPORT jint JNICALL Java_com_mi_audio_phrtf_AudioDetect_nativeDetect(
    JNIEnv *env, jobject thiz,
    jbyteArray l1, jbyteArray l2,
    jbyteArray r1, jbyteArray r2,
    jbyteArray c1, jbyteArray c2)
{
    (void)thiz;
    __android_log_print(ANDROID_LOG_ERROR, "JNITEST", "phrtf start. \n");

    float *data_inL1 = byteArray2FloatArray(env, l1, 16);
    float *data_inL2 = byteArray2FloatArray(env, l2, 16);
    float *data_inC1 = byteArray2FloatArray(env, c1, 16);
    float *data_inC2 = byteArray2FloatArray(env, c2, 16);
    float *data_inR1 = byteArray2FloatArray(env, r1, 16);
    float *data_inR2 = byteArray2FloatArray(env, r2, 16);

    void *phrtfPara = phrtf_init();

    uint8_t checkhrtf = hrtfWavCheck(
        phrtfPara,
        data_inL1, data_inL2,
        data_inC1, data_inC2,
        data_inR1, data_inR2
    );

    __android_log_print(ANDROID_LOG_ERROR, "JNITEST", "checkhrtf %d", (int)checkhrtf);

    int indexStamp = 0;
    if (checkhrtf == 111) {
        indexStamp = phrtf_process(phrtfPara, data_inC1, data_inL1, data_inR1);
    }

    if (hrtfData44k) {
        freeAndFill2DArray(hrtfData44k, rows);
        hrtfData44k = nullptr;
    }
    if (hrtfData48k) {
        freeAndFill2DArray(hrtfData48k, rows);
        hrtfData48k = nullptr;
    }

    hrtfData44k = allocateAndFill2DArray(rows, cols, indexStamp, 44100, &inputShift44k);
    hrtfData48k = allocateAndFill2DArray(rows, cols, indexStamp, 48000, &inputShift48k);

    __android_log_print(ANDROID_LOG_ERROR, "JNITEST", "inputshift 44k %d 48k %d", inputShift44k, inputShift48k);

    phrtf_free(phrtfPara);

    if (data_inL1) free(data_inL1);
    if (data_inL2) free(data_inL2);
    if (data_inC1) free(data_inC1);
    if (data_inC2) free(data_inC2);
    if (data_inR1) free(data_inR1);
    if (data_inR2) free(data_inR2);

    return (jint)checkhrtf;
}

JNIEXPORT jobjectArray JNICALL Java_com_mi_audio_phrtf_AudioDetect_getHrtfData44k(JNIEnv *env, jobject thiz)
{
    (void)thiz;
    jclass intArrayClass = env->FindClass("[I");
    if (!intArrayClass) return nullptr;

    jobjectArray result = env->NewObjectArray(rows, intArrayClass, nullptr);
    if (!result) return nullptr;

    for (int i = 0; i < rows; i++) {
        jintArray intArray = env->NewIntArray(cols);
        if (!intArray) return nullptr;

        if (hrtfData44k && hrtfData44k[i]) {
            env->SetIntArrayRegion(intArray, 0, cols, (const jint *)hrtfData44k[i]);
        }
        env->SetObjectArrayElement(result, i, intArray);
        env->DeleteLocalRef(intArray);
    }
    return result;
}

JNIEXPORT jobjectArray JNICALL Java_com_mi_audio_phrtf_AudioDetect_getHrtfData48k(JNIEnv *env, jobject thiz)
{
    (void)thiz;
    jclass intArrayClass = env->FindClass("[I");
    if (!intArrayClass) return nullptr;

    jobjectArray result = env->NewObjectArray(rows, intArrayClass, nullptr);
    if (!result) return nullptr;

    for (int i = 0; i < rows; i++) {
        jintArray intArray = env->NewIntArray(cols);
        if (!intArray) return nullptr;

        if (hrtfData48k && hrtfData48k[i]) {
            env->SetIntArrayRegion(intArray, 0, cols, (const jint *)hrtfData48k[i]);
        }
        env->SetObjectArrayElement(result, i, intArray);
        env->DeleteLocalRef(intArray);
    }
    return result;
}

JNIEXPORT jint JNICALL Java_com_mi_audio_phrtf_AudioDetect_getInputShift44k(JNIEnv *env, jobject thiz)
{
    (void)env;
    (void)thiz;
    return inputShift44k;
}

JNIEXPORT jint JNICALL Java_com_mi_audio_phrtf_AudioDetect_getInputShift48k(JNIEnv *env, jobject thiz)
{
    (void)env;
    (void)thiz;
    return inputShift48k;
}

} /* extern "C" */
