/**
 * 1:1 C++ Reconstruction of libaivsopus.so
 * Target: Xiaomi AIVS (AI Voice Service) Opus Codec and Stream Synchronization Engine
 * Official Java Class: com.xiaomi.aivsbluetoothsdk.voice.OpusManager
 */

#include <jni.h>
#include <opus/opus.h>
#include <semaphore.h>
#include <pthread.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <stdint.h>

#ifdef __ANDROID__
#include <android/log.h>
#else
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

#define LOG_TAG "jni_opus"

// Protocol Constants
#define AIVS_RING_BUFFER_SIZE  10240 // 0x2800 bytes
#define AIVS_MAX_FRAME_LEN     401   // 0x191 bytes max
#define AIVS_SAMPLE_RATE       16000
#define AIVS_CHANNELS          1
#define AIVS_FRAME_SAMPLES     320   // 20 ms at 16 kHz
#define AIVS_PCM_FRAME_BYTES   640   // 320 * 2 bytes
#define AIVS_MAX_PCM_OUT       1920  // 0x780 samples

// 4-byte Synchronization Magic Header: 0xAA, 0xEA, 0xBD, 0xAC
static const uint8_t SYNC_HEADER[4] = { 0xAA, 0xEA, 0xBD, 0xAC };

// Ring buffer structure matching FUN_0010a740 in libaivsopus.so
typedef struct {
    uint8_t buffer[AIVS_RING_BUFFER_SIZE];
    uint32_t head;
    uint32_t tail;
    uint32_t size;
    pthread_mutex_t lock;
} RingBuffer;

static RingBuffer g_ring_buf;
static sem_t g_stream_sem;
static JavaVM *g_jvm = NULL;
static jobject g_opus_manager_obj = NULL;
static jmethodID g_onDecodeStreamReceive_mid = NULL;
static OpusDecoder *g_stream_decoder = NULL;
static volatile int g_dec_status = 0; // 0 = stopped, 1 = running

// =============================================================================
// Ring Buffer Helpers (FUN_0010a740, FUN_0010a728, FUN_0010a634, FUN_0010a888)
// =============================================================================

static void ring_buffer_init(RingBuffer *rb) {
    rb->head = 0;
    rb->tail = 0;
    rb->size = 0;
    pthread_mutex_init(&rb->lock, NULL);
}

static uint32_t ring_buffer_free_space(RingBuffer *rb) {
    pthread_mutex_lock(&rb->lock);
    uint32_t free_space = AIVS_RING_BUFFER_SIZE - rb->size;
    pthread_mutex_unlock(&rb->lock);
    return free_space;
}

static void ring_buffer_write(RingBuffer *rb, const uint8_t *data, uint32_t len) {
    pthread_mutex_lock(&rb->lock);
    uint32_t to_write = len;
    if (to_write > AIVS_RING_BUFFER_SIZE - rb->size) {
        to_write = AIVS_RING_BUFFER_SIZE - rb->size;
    }
    for (uint32_t i = 0; i < to_write; i++) {
        rb->buffer[rb->tail] = data[i];
        rb->tail = (rb->tail + 1) % AIVS_RING_BUFFER_SIZE;
    }
    rb->size += to_write;
    pthread_mutex_unlock(&rb->lock);
}

static int ring_buffer_read_exact(RingBuffer *rb, uint32_t len, uint8_t *dst) {
    while (g_dec_status != 0) {
        pthread_mutex_lock(&rb->lock);
        if (rb->size >= len) {
            for (uint32_t i = 0; i < len; i++) {
                dst[i] = rb->buffer[rb->head];
                rb->head = (rb->head + 1) % AIVS_RING_BUFFER_SIZE;
            }
            rb->size -= len;
            pthread_mutex_unlock(&rb->lock);
            return 1;
        }
        pthread_mutex_unlock(&rb->lock);

        // Not enough data, wait on semaphore
        sem_wait(&g_stream_sem);
    }
    return 0;
}

// =============================================================================
// Java Callback Wrapper (FUN_0010a994)
// =============================================================================

static void notify_java_decode_received(const opus_int16 *pcm_data, int pcm_byte_len) {
    if (g_jvm == NULL || g_opus_manager_obj == NULL || g_onDecodeStreamReceive_mid == NULL) {
        return;
    }

    JNIEnv *env = NULL;
    int attached = 0;
    jint get_env_res = g_jvm->GetEnv((void **)&env, JNI_VERSION_1_6);

    if (get_env_res == JNI_EDETACHED) {
        if (g_jvm->AttachCurrentThread((void **)&env, NULL) != 0) {
            __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "AttachCurrentThread failed");
            return;
        }
        attached = 1;
    } else if (get_env_res != JNI_OK) {
        return;
    }

    jbyteArray byte_array = env->NewByteArray(pcm_byte_len);
    if (byte_array != NULL) {
        env->SetByteArrayRegion(byte_array, 0, pcm_byte_len, (const jbyte *)pcm_data);
        env->CallVoidMethod(g_opus_manager_obj, g_onDecodeStreamReceive_mid, pcm_byte_len, byte_array);
        env->DeleteLocalRef(byte_array);
    }

    if (attached) {
        g_jvm->DetachCurrentThread();
    }
}

// =============================================================================
// Native JNI Implementations (Table at 0x00153640 in libaivsopus.so)
// =============================================================================

static jboolean native_initNativeID(JNIEnv *env, jobject thiz) {
    if (g_opus_manager_obj != NULL) {
        env->DeleteGlobalRef(g_opus_manager_obj);
    }
    g_opus_manager_obj = env->NewGlobalRef(thiz);

    jclass cls = env->GetObjectClass(thiz);
    if (cls == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Unable to find class 'com/xiaomi/aivsbluetoothsdk/voice/OpusManager'");
        return JNI_FALSE;
    }

    ring_buffer_init(&g_ring_buf);
    sem_init(&g_stream_sem, 0, 0);

    g_onDecodeStreamReceive_mid = env->GetMethodID(cls, "onDecodeStreamReceive", "(I[B)V");
    if (g_onDecodeStreamReceive_mid == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "The calling class does not implement all necessary interface methods");
        return JNI_FALSE;
    }

    return JNI_TRUE;
}

static void native_saveAudioSteam(JNIEnv *env, jobject thiz, jbyteArray streamBytes) {
    (void)thiz;
    if (env == NULL || streamBytes == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "save Stream failed!");
        return;
    }

    jsize len = env->GetArrayLength(streamBytes);
    if (len <= 0) return;

    jbyte *bytes = env->GetByteArrayElements(streamBytes, NULL);
    if (bytes == NULL) return;

    uint32_t free_space = ring_buffer_free_space(&g_ring_buf);
    if (free_space < (uint32_t)len) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "save buf size fail:size:%d.", len);
    } else {
        ring_buffer_write(&g_ring_buf, (const uint8_t *)bytes, len);
    }

    __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "sem notify......");
    sem_post(&g_stream_sem);

    env->ReleaseByteArrayElements(streamBytes, bytes, JNI_ABORT);
}

static void native_decodeAudioStream(JNIEnv *env, jobject thiz, jint status) {
    (void)env;
    (void)thiz;

    if (g_dec_status == status) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, 
            "---decodeAudioStream-- wrong decode status. current speex_dec_status : %d  request op state:%d ", status, status);
        if (g_dec_status != 1) return;
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "force stop decode stream: dec_status: %d ", 1);
        g_dec_status = 0;
        return;
    }

    g_dec_status = status;
    if (status != 1) {
        sem_post(&g_stream_sem);
        return;
    }

    __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "decodeAudioStream opus_dec_status = %d", status);

    if (g_stream_decoder != NULL) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "opus decode init NOT free.destory first!");
        opus_decoder_destroy(g_stream_decoder);
        g_stream_decoder = NULL;
    }

    int err = 0;
    g_stream_decoder = opus_decoder_create(AIVS_SAMPLE_RATE, AIVS_CHANNELS, &err);
    if (g_stream_decoder == NULL || err != OPUS_OK) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "opus decode init failed!");
        return;
    }

    uint8_t sync_buf[4];
    uint8_t len_buf[2];
    uint8_t opus_frame[AIVS_MAX_FRAME_LEN];
    opus_int16 pcm_out[AIVS_MAX_PCM_OUT];

    while (g_dec_status != 0) {
        // Step 1: Search for sync magic: 0xAA 0xEA 0xBD 0xAC
        if (!ring_buffer_read_exact(&g_ring_buf, 4, sync_buf)) break;

        while (sync_buf[0] != SYNC_HEADER[0] || 
               sync_buf[1] != SYNC_HEADER[1] || 
               sync_buf[2] != SYNC_HEADER[2] || 
               sync_buf[3] != SYNC_HEADER[3]) {
            if (g_dec_status == 0) break;
            // Shift 1 byte
            sync_buf[0] = sync_buf[1];
            sync_buf[1] = sync_buf[2];
            sync_buf[2] = sync_buf[3];
            if (!ring_buffer_read_exact(&g_ring_buf, 1, &sync_buf[3])) break;
        }

        if (g_dec_status == 0) break;

        // Step 2: Read 2-byte frame length
        if (!ring_buffer_read_exact(&g_ring_buf, 2, len_buf)) break;
        uint16_t frame_len = (uint16_t)len_buf[0] | ((uint16_t)len_buf[1] << 8);

        if (frame_len == 0 || frame_len >= AIVS_MAX_FRAME_LEN) {
            __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "....find frame head.... frame_len : %d over limit", frame_len);
            continue;
        }

        // Step 3: Read frame_len bytes of Opus payload
        if (!ring_buffer_read_exact(&g_ring_buf, frame_len, opus_frame)) break;

        // Step 4: Decode Opus payload into PCM
        int decoded_samples = opus_decode(
            g_stream_decoder, 
            opus_frame, 
            frame_len, 
            pcm_out, 
            AIVS_MAX_PCM_OUT, 
            0
        );

        if (decoded_samples > 0) {
            notify_java_decode_received(pcm_out, decoded_samples * sizeof(opus_int16));
        } else {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "error: opus_decode return %d", decoded_samples);
        }
    }

    if (g_stream_decoder != NULL) {
        opus_decoder_destroy(g_stream_decoder);
        g_stream_decoder = NULL;
    }

    __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "finish this decode process!");
}

static jint native_encodeAudioFile(JNIEnv *env, jobject thiz, jstring inWavPath, jstring outOpusPath) {
    (void)thiz;
    if (inWavPath == NULL || outOpusPath == NULL) return -1;

    const char *in_path = env->GetStringUTFChars(inWavPath, NULL);
    const char *out_path = env->GetStringUTFChars(outOpusPath, NULL);

    FILE *fin = fopen(in_path, "rb");
    FILE *fout = fopen(out_path, "wb");

    if (!fin || !fout) {
        if (fin) fclose(fin);
        if (fout) fclose(fout);
        env->ReleaseStringUTFChars(inWavPath, in_path);
        env->ReleaseStringUTFChars(outOpusPath, out_path);
        return -1;
    }

    int err = 0;
    OpusEncoder *enc = opus_encoder_create(
        AIVS_SAMPLE_RATE, 
        AIVS_CHANNELS, 
        OPUS_APPLICATION_RESTRICTED_LOWDELAY, // 2051
        &err
    );

    if (!enc || err != OPUS_OK) {
        fclose(fin); fclose(fout);
        env->ReleaseStringUTFChars(inWavPath, in_path);
        env->ReleaseStringUTFChars(outOpusPath, out_path);
        return -1;
    }

    // Configure Opus parameters matching libaivsopus.so disassembly at 0x0010adf8
    opus_encoder_ctl(enc, OPUS_SET_COMPLEXITY(0));
    opus_encoder_ctl(enc, OPUS_SET_BITRATE(25000));
    opus_encoder_ctl(enc, OPUS_SET_SIGNAL(OPUS_SIGNAL_VOICE)); // 3001
    opus_encoder_ctl(enc, OPUS_SET_VBR(0)); // CBR mode

    opus_int16 pcm_chunk[AIVS_FRAME_SAMPLES];
    uint8_t opus_out[AIVS_MAX_FRAME_LEN];

    while (!feof(fin)) {
        size_t read_bytes = fread(pcm_chunk, 1, AIVS_PCM_FRAME_BYTES, fin);
        if (read_bytes < AIVS_PCM_FRAME_BYTES) break;

        int encoded_len = opus_encode(enc, pcm_chunk, AIVS_FRAME_SAMPLES, opus_out, sizeof(opus_out));
        if (encoded_len < 0) break;

        // Write AIVS Packet: [0xAA 0xEA 0xBD 0xAC] [len: uint16 LE] [opus_data]
        fwrite(SYNC_HEADER, 1, 4, fout);
        uint16_t len_u16 = (uint16_t)encoded_len;
        fwrite(&len_u16, 1, 2, fout);
        fwrite(opus_out, 1, encoded_len, fout);
    }

    opus_encoder_destroy(enc);
    fclose(fin);
    fclose(fout);

    env->ReleaseStringUTFChars(inWavPath, in_path);
    env->ReleaseStringUTFChars(outOpusPath, out_path);
    return 0;
}

static jint native_decodeAudioFile(JNIEnv *env, jobject thiz, jstring inOpusPath, jstring outWavPath) {
    (void)thiz;
    if (inOpusPath == NULL || outWavPath == NULL) return -1;

    const char *in_path = env->GetStringUTFChars(inOpusPath, NULL);
    const char *out_path = env->GetStringUTFChars(outWavPath, NULL);

    FILE *fin = fopen(in_path, "rb");
    FILE *fout = fopen(out_path, "wb");

    if (!fin || !fout) {
        if (fin) fclose(fin);
        if (fout) fclose(fout);
        env->ReleaseStringUTFChars(inOpusPath, in_path);
        env->ReleaseStringUTFChars(outWavPath, out_path);
        return -1;
    }

    int err = 0;
    OpusDecoder *dec = opus_decoder_create(AIVS_SAMPLE_RATE, AIVS_CHANNELS, &err);
    if (!dec || err != OPUS_OK) {
        fclose(fin); fclose(fout);
        env->ReleaseStringUTFChars(inOpusPath, in_path);
        env->ReleaseStringUTFChars(outWavPath, out_path);
        return -1;
    }

    uint8_t sync_buf[4];
    uint16_t frame_len = 0;
    uint8_t opus_packet[AIVS_MAX_FRAME_LEN];
    opus_int16 pcm_out[AIVS_MAX_PCM_OUT];

    while (!feof(fin)) {
        if (fread(sync_buf, 1, 4, fin) < 4) break;

        if (memcmp(sync_buf, SYNC_HEADER, 4) != 0) {
            // Unaligned, seek 1 byte
            fseek(fin, -3, SEEK_CUR);
            continue;
        }

        if (fread(&frame_len, 1, 2, fin) < 2) break;
        if (frame_len == 0 || frame_len >= AIVS_MAX_FRAME_LEN) continue;

        if (fread(opus_packet, 1, frame_len, fin) < frame_len) break;

        int decoded_samples = opus_decode(dec, opus_packet, frame_len, pcm_out, AIVS_MAX_PCM_OUT, 0);
        if (decoded_samples > 0) {
            fwrite(pcm_out, sizeof(opus_int16), decoded_samples, fout);
        }
    }

    opus_decoder_destroy(dec);
    fclose(fin);
    fclose(fout);

    env->ReleaseStringUTFChars(inOpusPath, in_path);
    env->ReleaseStringUTFChars(outWavPath, out_path);
    return 0;
}

// =============================================================================
// JNI Method Table & JNI_OnLoad (Offset 0x00153640 & 0x0010aaf8 in libaivsopus.so)
// =============================================================================

static JNINativeMethod g_methods[] = {
    { (char *)"initNativeID",      (char *)"()Z",                                    (void *)native_initNativeID },
    { (char *)"encodeAudioFile",   (char *)"(Ljava/lang/String;Ljava/lang/String;)I", (void *)native_encodeAudioFile },
    { (char *)"decodeAudioFile",   (char *)"(Ljava/lang/String;Ljava/lang/String;)I", (void *)native_decodeAudioFile },
    { (char *)"decodeAudioStream", (char *)"(I)V",                                    (void *)native_decodeAudioStream },
    { (char *)"saveAudioSteam",    (char *)"([B)V",                                  (void *)native_saveAudioSteam }
};

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)reserved;
    g_jvm = vm;

    JNIEnv *env = NULL;
    if (vm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Could not retrieve JNIEnv");
        return 0;
    }

    jclass cls = env->FindClass("com/xiaomi/aivsbluetoothsdk/voice/OpusManager");
    if (cls == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Native registration unable to find class 'com/xiaomi/aivsbluetoothsdk/voice/OpusManager'");
        return -1;
    }

    if (env->RegisterNatives(cls, g_methods, sizeof(g_methods) / sizeof(g_methods[0])) < 0) {
        return -1;
    }

    return JNI_VERSION_1_6;
}
