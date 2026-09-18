CXX ?= g++
CXXFLAGS ?= -O3 -fPIC -Wall -Wextra -std=c++17

# JDK JNI Includes
JAVA_HOME ?= /usr/lib/jvm/java-26-openjdk
JNI_INCLUDES = -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux

# Opus Includes & Libs
OPUS_CFLAGS = $(shell pkg-config --cflags opus 2>/dev/null || echo "-I/usr/include/opus")
OPUS_LIBS = $(shell pkg-config --libs opus 2>/dev/null || echo "-lopus")

# Speex Includes & Libs
SPEEX_CFLAGS = $(shell pkg-config --cflags speex 2>/dev/null || echo "-I/usr/include")
SPEEX_LIBS = $(shell pkg-config --libs speex 2>/dev/null || echo "-lspeex")

# LC3 Includes & Libs
LC3_CFLAGS = $(shell pkg-config --cflags lc3 2>/dev/null || echo "-I/usr/include")
LC3_LIBS = $(shell pkg-config --libs lc3 2>/dev/null || echo "-llc3")

CC ?= gcc
CFLAGS ?= -O3 -fPIC -Wall -Wextra

LDFLAGS_OPUS = -shared -lpthread $(OPUS_LIBS)
LDFLAGS_SPEEX = -shared -lpthread $(SPEEX_LIBS)
LDFLAGS_LC3 = -shared -lpthread $(LC3_LIBS)
LDFLAGS_PHRTF = -shared -lm

BUILD_DIR = build
TARGETS = $(BUILD_DIR)/liblog.so $(BUILD_DIR)/libjni_byopus.so $(BUILD_DIR)/libaivsopus.so $(BUILD_DIR)/libjlspeex.so $(BUILD_DIR)/libjni_lc3.so $(BUILD_DIR)/libphrtf.so $(BUILD_DIR)/libaudio_detect.so $(BUILD_DIR)/libxm_bluetooth.so

all: $(BUILD_DIR) $(TARGETS)

$(BUILD_DIR):
	mkdir -p $(BUILD_DIR)

$(BUILD_DIR)/liblog.so: log/log.c
	$(CC) $(CFLAGS) $< -o $@ -shared

$(BUILD_DIR)/libjni_byopus.so: opus/libjni_byopus.cpp
	$(CXX) $(CXXFLAGS) $(JNI_INCLUDES) $(OPUS_CFLAGS) $< -o $@ $(LDFLAGS_OPUS)

$(BUILD_DIR)/libaivsopus.so: opus/libaivsopus.cpp
	$(CXX) $(CXXFLAGS) $(JNI_INCLUDES) $(OPUS_CFLAGS) $< -o $@ $(LDFLAGS_OPUS)

$(BUILD_DIR)/libjlspeex.so: jlspeex/libjlspeex.cpp
	$(CXX) $(CXXFLAGS) $(JNI_INCLUDES) $(SPEEX_CFLAGS) $< -o $@ $(LDFLAGS_SPEEX)

$(BUILD_DIR)/libjni_lc3.so: lc3/libjni_lc3.cpp
	$(CXX) $(CXXFLAGS) $(JNI_INCLUDES) $(LC3_CFLAGS) $< -o $@ $(LDFLAGS_LC3)

PHRTF_SRCS = phrtf/phrtf.c phrtf/func.c phrtf/common.c phrtf/fast_conv.c phrtf/kiss_fft.c phrtf/kiss_fftr.c phrtf/tables.S

$(BUILD_DIR)/libphrtf.so: $(PHRTF_SRCS)
	$(CC) $(CFLAGS) -Wa,-Iphrtf -Iphrtf $^ -o $@ $(LDFLAGS_PHRTF)

$(BUILD_DIR)/libaudio_detect.so: audio_detect/audiodetect.cpp $(BUILD_DIR)/libphrtf.so
	$(CXX) $(CXXFLAGS) $(JNI_INCLUDES) -Iphrtf $< -o $@ -L$(BUILD_DIR) -lphrtf -shared -Wl,-rpath,'$$ORIGIN'

$(BUILD_DIR)/libxm_bluetooth.so: xm_bluetooth/xm_bluetooth.c
	$(CC) $(CFLAGS) $(JNI_INCLUDES) $< -o $@ -shared

clean:
	rm -rf $(BUILD_DIR)

.PHONY: all clean


