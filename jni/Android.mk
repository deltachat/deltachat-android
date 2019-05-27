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
LOCAL_LDLIBS 	:= -ljnigraphics -llog -lz -latomic
LOCAL_STATIC_LIBRARIES :=  deltachat-core
# if you get "undefined reference" errors, the reason for this may be the _order_! Eg. libiconv as the first library does not work!
# "breakpad" was placed after "crypto", NativeLoader.cpp after dc_wrapper.c

LOCAL_CFLAGS 	:= -w -Os -DNULL=0 -DSOCKLEN_T=socklen_t -DLOCALE_NOT_USED -D_LARGEFILE_SOURCE=1 -D_FILE_OFFSET_BITS=64
LOCAL_CFLAGS 	+= -Drestrict='' -D__EMX__ -DOPUS_BUILD -DFIXED_POINT -DUSE_ALLOCA -DHAVE_LRINT -DHAVE_LRINTF -fno-math-errno -std=c99
LOCAL_CFLAGS 	+= -DANDROID_NDK -DDISABLE_IMPORTGL -fno-strict-aliasing -fprefetch-loop-arrays -DAVOID_TABLES -DANDROID_TILE_BASED_DECODE -DANDROID_ARMV6_IDCT -ffast-math -D__STDC_CONSTANT_MACROS

LOCAL_SRC_FILES := \
utils/org_thoughtcrime_securesms_util_FileUtils.cpp \
dc_wrapper.c

include $(BUILD_SHARED_LIBRARY)
