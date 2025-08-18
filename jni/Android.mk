JNI_DIR := $(call my-dir)
LOCAL_PATH := $(call my-dir)

# Include prebuilt rust

include $(CLEAR_VARS)
LOCAL_MODULE            := deltachat-core
LOCAL_SRC_FILES         := $(TARGET_ARCH_ABI)/libdeltachat.a
# The header files should be located in the following dir relative to jni/ dir
LOCAL_EXPORT_C_INCLUDES := include/
include $(PREBUILT_STATIC_LIBRARY)


################################################################################
# main shared library as used from Java (includes the static ones)
################################################################################

include $(CLEAR_VARS)

LOCAL_MODULE     := native-utils

LOCAL_C_INCLUDES := $(JNI_DIR)/utils/
LOCAL_LDLIBS := -llog
LOCAL_STATIC_LIBRARIES :=  deltachat-core

# -Werror flag is important to catch incompatibilities between the JNI bindings and the core.
# Otherwise passing a variable of different type such as char * instead of int
# causes only a -Wint-conversion warning.
LOCAL_CFLAGS 	:= -Werror -Wno-pointer-to-int-cast -Wno-int-to-pointer-cast -DNULL=0 -DSOCKLEN_T=socklen_t -DLOCALE_NOT_USED -D_LARGEFILE_SOURCE=1 -D_FILE_OFFSET_BITS=64
LOCAL_CFLAGS 	+= -Drestrict='' -D__EMX__ -DFIXED_POINT -DUSE_ALLOCA -DHAVE_LRINT -DHAVE_LRINTF -fno-math-errno
LOCAL_CFLAGS 	+= -DANDROID_NDK -DDISABLE_IMPORTGL -fno-strict-aliasing -DAVOID_TABLES -DANDROID_TILE_BASED_DECODE -DANDROID_ARMV6_IDCT -ffast-math -D__STDC_CONSTANT_MACROS

LOCAL_SRC_FILES := dc_wrapper.c
LOCAL_LDFLAGS += -Wl,--build-id=none

include $(BUILD_SHARED_LIBRARY)
