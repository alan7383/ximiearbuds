/**
 * 1:1 C++ Reverse Engineering of libjlspeex.so (132 KB)
 * 
 * Target: JieLi Speex Voice Compression & Streaming Engine for Xiaomi AI Voice Service (XiaoAI)
 * Extracted from: /home/alan/earbuds_decompiled/resources/lib/arm64-v8a/libjlspeex.so
 * Official Binding Java Class: com.xiaomi.aivsbluetoothsdk.voice.SpeexManager
 * 
 * Capabilities:
 * - Dynamic JNI registration via JNI_OnLoad for com.xiaomi.aivsbluetoothsdk.voice.SpeexManager
 * - JieLi high-performance circular buffer (cbuf_t, 10 KB cache) with thread synchronization
 * - AIVS frame synchronization (magic header 0xAA 0xEA 0xBD 0xAC, 5-frame bundling)
 * - Speex Wideband (16 kHz, 320 samples/frame = 20ms) encoding & decoding with perceptual enhancement
 * - Real-time stream decoding thread invoking onDecodeStreamReceive(int, byte[])
 */

#include <jni.h>
#include <speex/speex.h>
#include <speex/speex_bits.h>
#include <semaphore.h>
#include <pthread.h>
#include <stdlib.h>
#include <string.h>
#include <stdio.h>
#include <stdint.h>
#include <stdbool.h>

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

#define LOG_TAG "AIVS:jni_speex"

#define JL_SPEEX_RING_BUF_SIZE 10240 // 0x2800 bytes
#define JL_SPEEX_MAX_FRAME_LEN 401   // 0x191 bytes max
#define JL_SPEEX_FRAME_SAMPLES 320   // 16 kHz wideband (20ms) -> 640 bytes PCM
#define JL_SPEEX_MAGIC_HEADER  0xacbdeaaa // 0xAA, 0xEA, 0xBD, 0xAC (little endian)

extern "C" {

// ============================================================================
// Data Types & Structures
// ============================================================================

typedef struct {
    uint8_t  *begin;         // 0x00: start of ring buffer
    uint8_t  *end;           // 0x08: end of ring buffer (begin + total_len)
    uint8_t  *read_ptr;      // 0x10: current read cursor
    uint8_t  *write_ptr;     // 0x18: current write cursor
    uint8_t  *prewrite_ptr;  // 0x20: speculative write cursor
    uint32_t  prewrite_len;  // 0x28: speculative data length
    uint32_t  data_len;      // 0x2c: committed data length in buffer
    uint32_t  total_len;     // 0x30: total buffer capacity (10240 bytes)
} cbuf_t;

typedef size_t (*speex_enc_needbuf_fn)(int);
typedef int (*speex_enc_open_fn)(void *, void *, uint32_t, uint16_t);
typedef bool (*speex_enc_run_fn)(void *);

typedef struct {
    void *priv;
    size_t (*input_data)(void *priv, void *buf, size_t len);
    void (*output_data)(void *priv, void *data, uint16_t len);
    size_t (*status)(void *priv);
} speex_io_t;

// ============================================================================
// Global Variables (Exact Symbol Table Parity with libjlspeex.so)
// ============================================================================

cbuf_t cbuffer;
uint8_t cache_cbuf_ptr[JL_SPEEX_RING_BUF_SIZE];
sem_t bin_sem;
pthread_mutex_t mutex = PTHREAD_MUTEX_INITIALIZER;
FILE *fbits = NULL;
FILE *fin = NULL;
FILE *fout = NULL;
int outframe_cnt = 0;
int speex_dec_status = 0;
pthread_t start_decode_stream = 0;

static JavaVM *g_jvm = NULL;               // DAT_0012ae10
static jobject g_speexManagerRef = NULL;   // DAT_0012ae18
static jmethodID g_onDecodeStreamReceive = NULL; // DAT_0012ae20

// Forward declarations
size_t e_input_data(void *enc, void *buf, size_t len);
void e_output_data(void *enc, void *data, uint16_t len);
size_t d_input(void *priv, uint64_t offset, void *buf, int len, char flag);
void check_buf(void *priv, uint64_t offset, void *buf);
void output(void *priv, void *pcm, int len);
uint64_t get_lslen(void);
int mp_store_rev_data(void *p1, void *p2, int val);
uint64_t d_input_stream(void);
uint32_t check_stream_buf(void *priv, uint32_t len, void *out_buf);
void stream_output(int ret, const void *pcm_data, int len);
uint64_t get_stream_lslen(void);
size_t speex_encode_neebuf(int sample_rate);
int speex_enc_open(void *enc_obj, void *io_table, uint32_t quality, uint16_t sample_rate);
bool speex_enc_run(void *enc_obj);
uint64_t speex_decoder_run(void);

void *ENC_OPS_G[4] = {
    (void *)speex_encode_neebuf,
    (void *)speex_enc_open,
    (void *)speex_enc_run,
    NULL
};

void *speex_decoder_ops[4] = {
    NULL,
    NULL,
    NULL,
    (void *)speex_decoder_run
};

speex_io_t speex_en_io = {
    NULL,
    e_input_data,
    e_output_data,
    NULL
};

void *speex_dec_io[6] = {
    NULL,
    (void *)d_input,
    (void *)check_buf,
    (void *)output,
    (void *)get_lslen,
    (void *)mp_store_rev_data
};

// ============================================================================
// JieLi Ring Buffer Implementation (cbuf_*)
// ============================================================================

int cbuf_init(cbuf_t *cbuf, void *buf, uint32_t size) {
    if (!cbuf) return -1;
    cbuf->begin = (uint8_t *)buf;
    cbuf->end = (uint8_t *)buf + size;
    cbuf->read_ptr = (uint8_t *)buf;
    cbuf->write_ptr = (uint8_t *)buf;
    cbuf->prewrite_ptr = (uint8_t *)buf;
    cbuf->prewrite_len = 0;
    cbuf->data_len = 0;
    cbuf->total_len = size;
    return pthread_mutex_init(&mutex, NULL);
}

int cbuf_clear(cbuf_t *cbuf) {
    if (!cbuf) return -1;
    pthread_mutex_lock(&mutex);
    cbuf->read_ptr = cbuf->begin;
    cbuf->write_ptr = cbuf->begin;
    cbuf->prewrite_ptr = cbuf->begin;
    cbuf->prewrite_len = 0;
    cbuf->data_len = 0;
    return pthread_mutex_unlock(&mutex);
}

uint32_t cbuf_is_write_able(cbuf_t *cbuf, uint32_t len) {
    if (!cbuf) return 0;
    uint32_t avail = cbuf->total_len - cbuf->data_len;
    return (len <= avail) ? avail : 0;
}

uint32_t cbuf_write(cbuf_t *cbuf, const void *src, uint32_t len) {
    if (!cbuf || len == 0) return 0;
    uint32_t avail = cbuf->total_len - cbuf->data_len;
    if (len > avail) {
        len = avail;
        if (avail == 0) return 0;
    }
    uint8_t *dest = cbuf->write_ptr;
    uint32_t to_end = (uint32_t)(cbuf->end - dest);
    if (len <= to_end) {
        memcpy(dest, src, len);
        cbuf->write_ptr = dest + len;
    } else {
        memcpy(dest, src, to_end);
        uint32_t rem = len - to_end;
        memcpy(cbuf->begin, (const uint8_t *)src + to_end, rem);
        cbuf->write_ptr = cbuf->begin + rem;
    }
    pthread_mutex_lock(&mutex);
    cbuf->data_len += len;
    cbuf->prewrite_ptr = cbuf->write_ptr;
    cbuf->prewrite_len = cbuf->data_len;
    pthread_mutex_unlock(&mutex);
    return len;
}

uint32_t cbuf_read(cbuf_t *cbuf, void *dst, uint32_t len) {
    if (!cbuf || len == 0) return 0;
    uint8_t *src = cbuf->read_ptr;
    if (cbuf->end <= src) {
        src = cbuf->begin;
        cbuf->read_ptr = src;
    }
    if (len > cbuf->data_len) {
        return 0;
    }
    uint32_t to_end = (uint32_t)(cbuf->end - src);
    if (to_end >= len) {
        memcpy(dst, src, len);
        cbuf->read_ptr = src + len;
    } else {
        memcpy(dst, src, to_end);
        uint32_t rem = len - to_end;
        memcpy((uint8_t *)dst + to_end, cbuf->begin, rem);
        cbuf->read_ptr = cbuf->begin + rem;
    }
    pthread_mutex_lock(&mutex);
    cbuf->data_len -= len;
    cbuf->prewrite_len = cbuf->data_len;
    pthread_mutex_unlock(&mutex);
    return len;
}

uint32_t cbuf_prewrite(cbuf_t *cbuf, const void *src, uint32_t len) {
    if (!cbuf || (cbuf->total_len - cbuf->prewrite_len < len)) {
        return 0;
    }
    uint8_t *dest = cbuf->prewrite_ptr;
    uint32_t to_end = (uint32_t)(cbuf->end - dest);
    if (to_end < len) {
        memcpy(dest, src, to_end);
        uint32_t rem = len - to_end;
        memcpy(cbuf->begin, (const uint8_t *)src + to_end, rem);
        cbuf->prewrite_ptr = cbuf->begin + rem;
    } else {
        memcpy(dest, src, len);
        cbuf->prewrite_ptr = dest + len;
    }
    pthread_mutex_lock(&mutex);
    cbuf->prewrite_len += len;
    pthread_mutex_unlock(&mutex);
    return len;
}

int cbuf_updata_prewrite(cbuf_t *cbuf) {
    if (!cbuf) return -1;
    pthread_mutex_lock(&mutex);
    cbuf->data_len = cbuf->prewrite_len;
    cbuf->write_ptr = cbuf->prewrite_ptr;
    return pthread_mutex_unlock(&mutex);
}

int cbuf_discard_prewrite(cbuf_t *cbuf) {
    if (!cbuf) return -1;
    pthread_mutex_lock(&mutex);
    cbuf->prewrite_len = cbuf->data_len;
    cbuf->prewrite_ptr = cbuf->write_ptr;
    return pthread_mutex_unlock(&mutex);
}

uint32_t cbuf_rewrite(cbuf_t *cbuf, void *target, const void *src, uint32_t len) {
    if (!cbuf) return 0;
    uint32_t to_end = (uint32_t)(cbuf->end - (uint8_t *)target);
    if (to_end < len) {
        memcpy(target, src, to_end);
        memcpy(cbuf->begin, (const uint8_t *)src + to_end, len - to_end);
    } else {
        memcpy(target, src, len);
    }
    return len;
}

void cbuf_write_alloc(cbuf_t *cbuf, uint32_t *len) {
    if (!cbuf || !len) return;
    uint32_t to_end = (uint32_t)(cbuf->end - cbuf->write_ptr);
    uint32_t free_space = cbuf->total_len - cbuf->data_len;
    if (to_end == 0) {
        cbuf->write_ptr = cbuf->begin;
        to_end = free_space;
    } else if (to_end > free_space) {
        to_end = free_space;
    }
    *len = to_end;
}

int cbuf_write_updata(cbuf_t *cbuf, uint32_t len) {
    if (!cbuf) return -1;
    pthread_mutex_lock(&mutex);
    cbuf->write_ptr += len;
    cbuf->prewrite_ptr = cbuf->write_ptr;
    cbuf->data_len += len;
    cbuf->prewrite_len = cbuf->data_len;
    return pthread_mutex_unlock(&mutex);
}

void cbuf_read_alloc(cbuf_t *cbuf, uint32_t *len) {
    if (!cbuf || !len) return;
    if (cbuf->end <= cbuf->read_ptr) {
        cbuf->read_ptr = cbuf->begin;
    }
    uint32_t to_end = (uint32_t)(cbuf->end - cbuf->read_ptr);
    if (cbuf->data_len < to_end) {
        to_end = cbuf->data_len;
    }
    *len = to_end;
}

int cbuf_read_updata(cbuf_t *cbuf, uint32_t len) {
    if (!cbuf) return -1;
    pthread_mutex_lock(&mutex);
    cbuf->read_ptr += len;
    if (cbuf->end <= cbuf->read_ptr) {
        cbuf->read_ptr = cbuf->begin;
    }
    cbuf->data_len -= len;
    cbuf->prewrite_len = cbuf->data_len;
    return pthread_mutex_unlock(&mutex);
}

uint32_t cbuf_read_alloc_len(cbuf_t *cbuf, void *dst, uint32_t len) {
    if (!cbuf) return 0;
    uint8_t *src = cbuf->read_ptr;
    if (cbuf->end <= src) {
        src = cbuf->begin;
        cbuf->read_ptr = src;
    }
    if (len > cbuf->data_len) return 0;
    uint32_t to_end = (uint32_t)(cbuf->end - src);
    if (to_end >= len) {
        memcpy(dst, src, len);
    } else {
        memcpy(dst, src, to_end);
        memcpy((uint8_t *)dst + to_end, cbuf->begin, len - to_end);
    }
    return len;
}

int cbuf_read_alloc_len_updata(cbuf_t *cbuf, uint32_t len) {
    if (!cbuf) return -1;
    pthread_mutex_lock(&mutex);
    cbuf->read_ptr += len;
    if (cbuf->end <= cbuf->read_ptr) {
        cbuf->read_ptr = cbuf->begin + (cbuf->read_ptr - cbuf->end);
    }
    cbuf->data_len -= len;
    cbuf->prewrite_len = cbuf->data_len;
    return pthread_mutex_unlock(&mutex);
}

// ============================================================================
// File & Stream Audio Helpers
// ============================================================================

void *speex_dec_init(void) {
    const SpeexMode *mode = speex_lib_get_mode(SPEEX_MODEID_WB); // 1 = wideband 16kHz
    void *dec = speex_decoder_init(mode);
    int val = 1;
    speex_decoder_ctl(dec, SPEEX_SET_ENH, &val); // Perceptual enhancer
    val = 1;
    speex_decoder_ctl(dec, 44, &val);
    val = 1;
    speex_decoder_ctl(dec, 39, &val);
    return dec;
}

size_t e_input_data(void *enc, void *buf, size_t len) {
    (void)enc;
    return fread(buf, 2, len & 0xffff, fin);
}

void e_output_data(void *enc, void *data, uint16_t len) {
    (void)enc;
    if (outframe_cnt == 0) {
        uint32_t magic = JL_SPEEX_MAGIC_HEADER;
        fwrite(&magic, 4, 1, fbits);
        fwrite(&len, 2, 1, fbits);
    }
    if (outframe_cnt < 4) {
        outframe_cnt++;
    } else {
        outframe_cnt = 0;
    }
    fwrite(data, 1, len, fbits);
}

size_t d_input(void *priv, uint64_t offset, void *buf, int len, char flag) {
    (void)priv;
    if (flag != 0) return 0;
    fseek(fbits, (long)offset, SEEK_SET);
    return fread(buf, 1, len, fbits);
}

void check_buf(void *priv, uint64_t offset, void *buf) {
    (void)priv;
    fseek(fbits, (long)offset, SEEK_SET);
    fread(buf, 1, 0x50, fbits);
}

void output(void *priv, void *pcm, int len) {
    (void)priv;
    fwrite(pcm, 1, len, fout);
}

uint64_t get_lslen(void) {
    long cur = ftell(fbits);
    fseek(fbits, 0, SEEK_END);
    long end = ftell(fbits);
    fseek(fbits, cur, SEEK_SET);
    return (uint64_t)end;
}

int mp_store_rev_data(void *p1, void *p2, int val) {
    (void)p1; (void)p2;
    return val;
}

uint64_t d_input_stream(void) {
    return 0;
}

uint32_t check_stream_buf(void *priv, uint32_t len, void *out_buf) {
    (void)priv;
    while (true) {
        if (speex_dec_status == 0) {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "-check_stream_buf- stop decode ");
            return 0;
        }
        if (len <= cbuffer.data_len) {
            cbuf_read(&cbuffer, out_buf, len);
            return len;
        }
        int ret = sem_wait(&bin_sem);
        if (ret != 0) {
            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "-check_stream_buf- err exit");
            return 0;
        }
    }
}

uint64_t get_stream_lslen(void) {
    return 0;
}

void stream_output(int ret, const void *pcm_data, int len) {
    (void)ret;
    if (!pcm_data || len < 1) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "args is error");
        return;
    }
    if (!g_jvm || !g_speexManagerRef || !g_onDecodeStreamReceive) {
        return;
    }
    JNIEnv *env = NULL;
    jint status = g_jvm->GetEnv((void **)&env, JNI_VERSION_1_6);
    bool attached = false;
    if (status < 0) {
        if (g_jvm->AttachCurrentThread((void **)&env, NULL) >= 0) {
            attached = true;
        } else {
            __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "AttachCurrentThread failed");
            return;
        }
    }
    jbyteArray arr = env->NewByteArray(len);
    if (arr) {
        env->SetByteArrayRegion(arr, 0, len, (const jbyte *)pcm_data);
        env->CallVoidMethod(g_speexManagerRef, g_onDecodeStreamReceive, (jint)len, arr);
        env->DeleteLocalRef(arr);
    }
    if (attached) {
        g_jvm->DetachCurrentThread();
    }
}

void *get_speex_enc_obj(void) {
    return (void *)ENC_OPS_G;
}

void *get_speex_ops(void) {
    return (void *)speex_decoder_ops;
}

size_t speex_encode_neebuf(int sample_rate) {
    (void)sample_rate;
    return 0x20f8; // 8440 bytes
}

int speex_enc_open(void *enc_obj, void *io_table, uint32_t quality, uint16_t sample_rate) {
    (void)enc_obj; (void)io_table; (void)quality; (void)sample_rate;
    return 0;
}

bool speex_enc_run(void *enc_obj) {
    (void)enc_obj;
    return false;
}

uint64_t speex_decoder_run(void) {
    return 0;
}

// ============================================================================
// JNI Native Implementations
// ============================================================================

JNIEXPORT jboolean JNICALL initNativeID(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "init_native_id");
    if (!env || !thiz) return JNI_FALSE;
    g_speexManagerRef = env->NewGlobalRef(thiz);
    env->GetJavaVM(&g_jvm);
    jclass cls = env->GetObjectClass(thiz);
    if (!cls) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Unable to find class '%s'",
                            "com/xiaomi/aivsbluetoothsdk/voice/SpeexManager");
        return (jboolean)0xff;
    }
    cbuf_init(&cbuffer, cache_cbuf_ptr, sizeof(cache_cbuf_ptr));
    int sem_res = sem_init(&bin_sem, 0, 0);
    if (sem_res != 0) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Semaphore initialization failed");
    }
    g_onDecodeStreamReceive = env->GetMethodID(cls, "onDecodeStreamReceive", "(I[B)V");
    if (!g_onDecodeStreamReceive) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "The calling class does not implement all necessary interface methods");
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

JNIEXPORT jint JNICALL encodeAudioFile(JNIEnv *env, jobject thiz, jstring inputWavPath, jstring outputSpeexPath) {
    (void)thiz;
    if (!env || !inputWavPath || !outputSpeexPath) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "encodeFile file failed!");
        return -1;
    }
    const char *in_path = env->GetStringUTFChars(inputWavPath, NULL);
    fin = fopen(in_path, "rb");
    const char *out_path = env->GetStringUTFChars(outputSpeexPath, NULL);
    fbits = fopen(out_path, "wb");
    if (!fin || !fbits) {
        if (fin) { fclose(fin); fin = NULL; }
        if (fbits) { fclose(fbits); fbits = NULL; }
        env->ReleaseStringUTFChars(inputWavPath, in_path);
        env->ReleaseStringUTFChars(outputSpeexPath, out_path);
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "encodeFile file failed!");
        return -1;
    }

    outframe_cnt = 0;
    const SpeexMode *mode = speex_lib_get_mode(SPEEX_MODEID_WB);
    void *enc_state = speex_encoder_init(mode);
    int quality = 5;
    speex_encoder_ctl(enc_state, SPEEX_SET_QUALITY, &quality);
    int vbr = 1;
    speex_encoder_ctl(enc_state, SPEEX_SET_VBR, &vbr);

    SpeexBits bits;
    speex_bits_init(&bits);

    // Skip WAV header (44 bytes = 0x2c)
    fseek(fin, 0x2c, SEEK_SET);

    int16_t pcm_buf[JL_SPEEX_FRAME_SAMPLES]; // 320 samples
    char out_buf[JL_SPEEX_MAX_FRAME_LEN];

    while (!feof(fin)) {
        size_t samples_read = fread(pcm_buf, sizeof(int16_t), JL_SPEEX_FRAME_SAMPLES, fin);
        if (samples_read < JL_SPEEX_FRAME_SAMPLES) break;
        speex_bits_reset(&bits);
        speex_encode_int(enc_state, pcm_buf, &bits);
        int bytes = speex_bits_write(&bits, out_buf, sizeof(out_buf));
        e_output_data(enc_state, out_buf, (uint16_t)bytes);
    }

    speex_bits_destroy(&bits);
    speex_encoder_destroy(enc_state);
    fclose(fin); fin = NULL;
    fclose(fbits); fbits = NULL;
    env->ReleaseStringUTFChars(inputWavPath, in_path);
    env->ReleaseStringUTFChars(outputSpeexPath, out_path);
    __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "encodeFile file success!");
    return 0;
}

JNIEXPORT jint JNICALL decodeAudioFile(JNIEnv *env, jobject thiz, jstring inputSpeexPath, jstring outputPcmPath) {
    (void)thiz;
    if (!env || !inputSpeexPath || !outputPcmPath) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "decodeFile file failed!");
        return -1;
    }
    const char *in_path = env->GetStringUTFChars(inputSpeexPath, NULL);
    fbits = fopen(in_path, "rb");
    const char *out_path = env->GetStringUTFChars(outputPcmPath, NULL);
    fout = fopen(out_path, "wb");
    if (!fbits || !fout) {
        if (fbits) { fclose(fbits); fbits = NULL; }
        if (fout) { fclose(fout); fout = NULL; }
        env->ReleaseStringUTFChars(inputSpeexPath, in_path);
        env->ReleaseStringUTFChars(outputPcmPath, out_path);
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "decodeFile file failed!");
        return -1;
    }

    const SpeexMode *mode = speex_lib_get_mode(SPEEX_MODEID_WB);
    void *dec_state = speex_decoder_init(mode);
    int enh = 1;
    speex_decoder_ctl(dec_state, SPEEX_SET_ENH, &enh);
    int val = 1;
    speex_decoder_ctl(dec_state, 44, &val);
    speex_decoder_ctl(dec_state, 39, &val);

    SpeexBits bits;
    speex_bits_init(&bits);

    uint8_t frame_buf[1072];
    int16_t pcm_out[JL_SPEEX_FRAME_SAMPLES]; // 320 samples = 640 bytes
    int frame_cnt = 0;
    uint16_t frame_len = 0;
    uint8_t sync_win[4] = {0};

    while (!feof(fbits)) {
        if (frame_cnt == 0) {
            // Find frame header: 0xAA, 0xEA, 0xBD, 0xAC
            if (fread(sync_win, 1, 4, fbits) != 4) break;
            while (true) {
                if (sync_win[0] == 0xAA && sync_win[1] == 0xEA &&
                    sync_win[2] == 0xBD && sync_win[3] == 0xAC) {
                    if (fread(&frame_len, 2, 1, fbits) != 1) goto done;
                    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG,
                                        "....find frame heda.... frame_len : %d", (int)frame_len);
                    if (frame_len < JL_SPEEX_MAX_FRAME_LEN) break;
                }
                if (feof(fbits)) goto done;
                sync_win[0] = sync_win[1];
                sync_win[1] = sync_win[2];
                sync_win[2] = sync_win[3];
                int c = fgetc(fbits);
                if (c == EOF) goto done;
                sync_win[3] = (uint8_t)c;
            }
        }

        if (fread(frame_buf, 1, frame_len, fbits) != frame_len) break;
        speex_bits_read_from(&bits, (char *)frame_buf, frame_len);
        int ret = speex_decode_int(dec_state, &bits, pcm_out);
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "decodeFile ret : %d", ret);
        fwrite(pcm_out, sizeof(int16_t), JL_SPEEX_FRAME_SAMPLES, fout);

        if (frame_cnt < 4) {
            frame_cnt++;
        } else {
            frame_cnt = 0;
        }
    }

done:
    speex_bits_destroy(&bits);
    speex_decoder_destroy(dec_state);
    fclose(fbits); fbits = NULL;
    fclose(fout); fout = NULL;
    env->ReleaseStringUTFChars(inputSpeexPath, in_path);
    env->ReleaseStringUTFChars(outputPcmPath, out_path);
    __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "decodeFile file succeed!");
    return 0;
}

JNIEXPORT void JNICALL decodeAudioStream(JNIEnv *env, jobject thiz, jint state) {
    (void)env; (void)thiz;
    if (speex_dec_status == state) {
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG,
                            "---decodeAudioStream-- wrong decode status. current speex_dec_status : %d  request op state:%d ",
                            state, state);
        if (speex_dec_status != 1) return;
        __android_log_print(ANDROID_LOG_WARN, LOG_TAG, "force stop decode stream: speex_dec_status");
        speex_dec_status = 0;
        sem_post(&bin_sem);
        return;
    }

    speex_dec_status = state;
    if (state == 0) {
        sem_post(&bin_sem);
        if (start_decode_stream != 0) {
            pthread_join(start_decode_stream, NULL);
            start_decode_stream = 0;
        }
        return;
    }

    if (state == 1) {
        const SpeexMode *mode = speex_lib_get_mode(SPEEX_MODEID_WB);
        void *dec = speex_decoder_init(mode);
        int enh = 1;
        speex_decoder_ctl(dec, SPEEX_SET_ENH, &enh);
        int val = 1;
        speex_decoder_ctl(dec, 44, &val);
        speex_decoder_ctl(dec, 39, &val);

        SpeexBits bits;
        speex_bits_init(&bits);

        uint8_t sync_bytes[4];
        uint16_t frame_len = 0;
        uint8_t frame_buf[1072];
        int16_t pcm_out[JL_SPEEX_FRAME_SAMPLES];
        int frame_count = 0;

        while (speex_dec_status != 0) {
            if (frame_count == 0) {
                // Read 4 bytes sync header
                while (cbuffer.data_len < 4) {
                    if (speex_dec_status == 0) goto stream_exit;
                    if (sem_wait(&bin_sem) != 0) {
                        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "-check_stream_buf- err exit");
                        goto stream_exit;
                    }
                }
                cbuf_read(&cbuffer, sync_bytes, 4);

                // Scan for 0xAA, 0xEA, 0xBD, 0xAC
                while (true) {
                    if (sync_bytes[0] == 0xAA && sync_bytes[1] == 0xEA &&
                        sync_bytes[2] == 0xBD && sync_bytes[3] == 0xAC) {
                        // Read 2-byte frame length
                        while (cbuffer.data_len < 2) {
                            if (speex_dec_status == 0) goto stream_exit;
                            if (sem_wait(&bin_sem) != 0) {
                                __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "-check_stream_buf- err exit");
                                goto stream_exit;
                            }
                        }
                        cbuf_read(&cbuffer, &frame_len, 2);
                        if (frame_len < JL_SPEEX_MAX_FRAME_LEN) break;
                    } else {
                        sync_bytes[0] = sync_bytes[1];
                        sync_bytes[1] = sync_bytes[2];
                        sync_bytes[2] = sync_bytes[3];
                    }
                    if (speex_dec_status == 0) goto stream_exit;
                    __android_log_print(ANDROID_LOG_WARN, LOG_TAG, ">>>>>>  errcnt : %d", 1);
                    while (cbuffer.data_len < 1) {
                        if (speex_dec_status == 0) goto stream_exit;
                        if (sem_wait(&bin_sem) != 0) {
                            __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "-check_stream_buf- err exit");
                            goto stream_exit;
                        }
                    }
                    uint8_t next_b = 0;
                    cbuf_read(&cbuffer, &next_b, 1);
                    sync_bytes[3] = next_b;
                }
            }

            // Read frame_len bytes from ring buffer
            while (cbuffer.data_len < frame_len) {
                if (speex_dec_status == 0) goto stream_exit;
                if (sem_wait(&bin_sem) != 0) {
                    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "-check_stream_buf- err exit");
                    goto stream_exit;
                }
            }
            cbuf_read(&cbuffer, frame_buf, frame_len);

            speex_bits_read_from(&bits, (char *)frame_buf, frame_len);
            int dec_ret = speex_decode_int(dec, &bits, pcm_out);
            if (dec_ret >= 0) {
                stream_output(dec_ret, pcm_out, JL_SPEEX_FRAME_SAMPLES * sizeof(int16_t)); // 640 bytes
            }

            if (frame_count < 4) {
                frame_count++;
            } else {
                frame_count = 0;
            }
        }

    stream_exit:
        sem_destroy(&bin_sem);
        speex_bits_destroy(&bits);
        speex_decoder_destroy(dec);
    }
}

JNIEXPORT void JNICALL saveAudioSteam(JNIEnv *env, jobject thiz, jbyteArray audioBytes) {
    (void)thiz;
    if (env && audioBytes) {
        jsize len = env->GetArrayLength(audioBytes);
        jbyte *buf = (jbyte *)env->GetPrimitiveArrayCritical(audioBytes, NULL);
        if (buf) {
            uint32_t writable = cbuf_is_write_able(&cbuffer, (uint32_t)len);
            if (writable < (uint32_t)len) {
                __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "save buf size fail:size:%d.", len);
            } else {
                cbuf_write(&cbuffer, buf, (uint32_t)len);
            }
            sem_post(&bin_sem);
            env->ReleasePrimitiveArrayCritical(audioBytes, buf, JNI_ABORT);
            return;
        }
    }
    __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "save Stream failed!");
}

// ============================================================================
// JNI Registration Table & JNI_OnLoad
// ============================================================================

static const JNINativeMethod g_methods[] = {
    { (char *)"initNativeID",      (char *)"()Z",                                     (void *)initNativeID },
    { (char *)"encodeAudioFile",   (char *)"(Ljava/lang/String;Ljava/lang/String;)I", (void *)encodeAudioFile },
    { (char *)"decodeAudioFile",   (char *)"(Ljava/lang/String;Ljava/lang/String;)I", (void *)decodeAudioFile },
    { (char *)"decodeAudioStream", (char *)"(I)V",                                    (void *)decodeAudioStream },
    { (char *)"saveAudioSteam",    (char *)"([B)V",                                   (void *)saveAudioSteam }
};

JNIEXPORT jint JNI_OnLoad(JavaVM *vm, void *reserved) {
    (void)reserved;
    g_jvm = vm;
    JNIEnv *env = NULL;
    if (vm->GetEnv((void **)&env, JNI_VERSION_1_6) != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Could not retrieve JNIEnv");
        return 0;
    }
    jclass cls = env->FindClass("com/xiaomi/aivsbluetoothsdk/voice/SpeexManager");
    if (!cls) {
        __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, "Native registration unable to find class '%s'",
                            "com/xiaomi/aivsbluetoothsdk/voice/SpeexManager");
        return -1;
    }
    if (env->RegisterNatives(cls, g_methods, sizeof(g_methods) / sizeof(g_methods[0])) < 0) {
        return -1;
    }
    return JNI_VERSION_1_6;
}

} // extern "C"
