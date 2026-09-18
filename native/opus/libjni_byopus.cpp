/**
 * 1:1 C++ Reconstruction of libjni_byopus.so
 * Target: BaNyan Opus Decoder JNI Wrapper for Xiaomi Earbuds
 * Official Java Class: com.banya.opus.OpusDecoder
 */

#include <jni.h>
#include <opus/opus.h>
#include <stdlib.h>
#include <string.h>

#ifdef __ANDROID__
#include <android/log.h>
#else
#include <stdio.h>
#include <stdarg.h>
#define ANDROID_LOG_DEBUG 3
#define ANDROID_LOG_INFO  4
#define ANDROID_LOG_WARN  5
#define ANDROID_LOG_ERROR 6
static inline void __android_log_print(int prio, const char *tag, const char *fmt, ...) {
    (void)prio;
    va_list ap;
    va_start(ap, fmt);
    fprintf(stderr, "[%s] ", tag);
    vfprintf(stderr, fmt, ap);
    fprintf(stderr, "\n");
    va_end(ap);
}
#endif

#define LOG_TAG "jni_MixSdk"

// Global pointer table for decoder instances (DAT_00109040 in libjni_byopus.so)
static OpusDecoder *g_decoder_instances[1] = { NULL };
static JavaVM *g_jvm = NULL;

extern "C" {

JNIEXPORT jint JNICALL Java_com_banya_opus_OpusDecoder_createDecoder(
    JNIEnv *env, 
    jobject thiz, 
    jint sampleRate, 
    jint channels
) {
    (void)env;
    (void)thiz;

    if (g_decoder_instances[0] != NULL) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "exceed max decoders count!!! %d", 1);
        return -1;
    }

    int error = 0;
    OpusDecoder *decoder = opus_decoder_create(sampleRate, channels, &error);
    if (decoder != NULL && error == OPUS_OK) {
        g_decoder_instances[0] = decoder;
        return 0; // Handle index 0
    }

    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "failed to create decoder: %d", error);
    return -1;
}

JNIEXPORT jint JNICALL Java_com_banya_opus_OpusDecoder_decode(
    JNIEnv *env, 
    jobject thiz, 
    jint handleIndex, 
    jbyteArray inBytes, 
    jint inBytesLen, 
    jint sampleCount, 
    jbyteArray outBytes, 
    jint outBytesLen
) {
    (void)thiz;

    if (handleIndex != 0 || g_decoder_instances[0] == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "invalid decoder index: %d", handleIndex);
        return -1;
    }

    OpusDecoder *decoder = g_decoder_instances[handleIndex];

    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, 
                        "index:%d, len:%d, sampleCount:%d, outputSize:%d",
                        handleIndex, inBytesLen, sampleCount, outBytesLen);

    if (inBytesLen <= 0 || outBytesLen <= 0) {
        return -1;
    }

    void *inPtr = malloc(inBytesLen);
    if (inPtr == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "failed to alloc input buffer!");
        return -1;
    }

    void *outPtr = malloc(outBytesLen);
    if (outPtr == NULL) {
        free(inPtr);
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "failed to alloc output buffer!");
        return -1;
    }

    env->GetByteArrayRegion(inBytes, 0, inBytesLen, (jbyte *)inPtr);

    int decodedSamples = opus_decode(
        decoder, 
        (const unsigned char *)inPtr, 
        inBytesLen, 
        (opus_int16 *)outPtr, 
        sampleCount, 
        0 // decode_fec = 0
    );

    if (decodedSamples != sampleCount) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "failed to decode! num=%d", decodedSamples);
        free(inPtr);
        free(outPtr);
        return -1;
    }

    env->SetByteArrayRegion(outBytes, 0, outBytesLen, (const jbyte *)outPtr);

    free(inPtr);
    free(outPtr);
    return 0;
}

JNIEXPORT void JNICALL Java_com_banya_opus_OpusDecoder_destroyDecoder(
    JNIEnv *env, 
    jobject thiz, 
    jint handleIndex
) {
    (void)env;
    (void)thiz;

    if (handleIndex == 0 && g_decoder_instances[0] != NULL) {
        opus_decoder_destroy(g_decoder_instances[0]);
        g_decoder_instances[0] = NULL;
    }
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)reserved;
    g_jvm = vm;
    g_decoder_instances[0] = NULL;
    return JNI_VERSION_1_6;
}

} // extern "C"
