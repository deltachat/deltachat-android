LOCAL_PATH:= $(call my-dir)

################################################################################
# ffmpeg - libavformat
################################################################################

FFMPEG_PATH:= ./ffmpeg

local_src_files := \
	$(FFMPEG_PATH)/libavformat/cutils.c \
	$(FFMPEG_PATH)/libavformat/allformats.c \
	$(FFMPEG_PATH)/libavformat/file.c \
	$(FFMPEG_PATH)/libavformat/id3v1.c \
	$(FFMPEG_PATH)/libavformat/gifdec.c \
	$(FFMPEG_PATH)/libavformat/format.c \
	$(FFMPEG_PATH)/libavformat/metadata.c \
	$(FFMPEG_PATH)/libavformat/isom.c \
	$(FFMPEG_PATH)/libavformat/avio.c \
	$(FFMPEG_PATH)/libavformat/mov_chan.c \
	$(FFMPEG_PATH)/libavformat/options.c \
	$(FFMPEG_PATH)/libavformat/os_support.c \
	$(FFMPEG_PATH)/libavformat/dump.c \
	$(FFMPEG_PATH)/libavformat/qtpalette.c \
	$(FFMPEG_PATH)/libavformat/riff.c \
	$(FFMPEG_PATH)/libavformat/replaygain.c \
	$(FFMPEG_PATH)/libavformat/sdp.c \
	$(FFMPEG_PATH)/libavformat/url.c \
	$(FFMPEG_PATH)/libavformat/id3v2.c \
	$(FFMPEG_PATH)/libavformat/riffdec.c \
	$(FFMPEG_PATH)/libavformat/aviobuf.c \
	$(FFMPEG_PATH)/libavformat/mux.c \
	$(FFMPEG_PATH)/libavformat/mov.c \
	$(FFMPEG_PATH)/libavformat/utils.c

local_c_includes := \
        $(LOCAL_PATH)/$(FFMPEG_PATH) \
	$(LOCAL_PATH)/$(FFMPEG_PATH)/config/$(TARGET_ARCH_ABI)

LOCAL_SRC_FILES += $(local_src_files)
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_SRC_FILES += $(local_armv7_files)
else
    ifeq ($(TARGET_ARCH_ABI),armeabi)
       LOCAL_SRC_FILES += $(local_arm_files)
    else
        ifeq ($(TARGET_ARCH_ABI),x86)
           LOCAL_SRC_FILES += $(local_x86_files)
        endif
    endif
endif

LOCAL_CFLAGS += $(local_c_flags) -DPURIFY -DHAVE_AV_CONFIG_H -D_ISOC99_SOURCE -D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE -Dstrtod=avpriv_strtod -DPIC -DHAVE_AV_CONFIG_H -Os -DANDROID -fPIE -pie --static -std=c99
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_ARM_MODE:= arm
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= libavformat
include $(BUILD_STATIC_LIBRARY)


################################################################################
# ffmpeg - libavutil
################################################################################


include $(CLEAR_VARS)

local_src_files := \
	$(FFMPEG_PATH)/libavutil/adler32.c \
	$(FFMPEG_PATH)/libavutil/aes.c \
	$(FFMPEG_PATH)/libavutil/aes_ctr.c \
	$(FFMPEG_PATH)/libavutil/audio_fifo.c \
	$(FFMPEG_PATH)/libavutil/avstring.c \
	$(FFMPEG_PATH)/libavutil/base64.c \
	$(FFMPEG_PATH)/libavutil/blowfish.c \
	$(FFMPEG_PATH)/libavutil/bprint.c \
	$(FFMPEG_PATH)/libavutil/buffer.c \
	$(FFMPEG_PATH)/libavutil/camellia.c \
	$(FFMPEG_PATH)/libavutil/cast5.c \
	$(FFMPEG_PATH)/libavutil/channel_layout.c \
	$(FFMPEG_PATH)/libavutil/color_utils.c \
	$(FFMPEG_PATH)/libavutil/cpu.c \
	$(FFMPEG_PATH)/libavutil/crc.c \
	$(FFMPEG_PATH)/libavutil/des.c \
	$(FFMPEG_PATH)/libavutil/dict.c \
	$(FFMPEG_PATH)/libavutil/display.c \
	$(FFMPEG_PATH)/libavutil/downmix_info.c \
	$(FFMPEG_PATH)/libavutil/error.c \
	$(FFMPEG_PATH)/libavutil/eval.c \
	$(FFMPEG_PATH)/libavutil/fifo.c \
	$(FFMPEG_PATH)/libavutil/file.c \
	$(FFMPEG_PATH)/libavutil/file_open.c \
	$(FFMPEG_PATH)/libavutil/fixed_dsp.c \
	$(FFMPEG_PATH)/libavutil/float_dsp.c \
	$(FFMPEG_PATH)/libavutil/frame.c \
	$(FFMPEG_PATH)/libavutil/hash.c \
	$(FFMPEG_PATH)/libavutil/hmac.c \
	$(FFMPEG_PATH)/libavutil/imgutils.c \
	$(FFMPEG_PATH)/libavutil/integer.c \
	$(FFMPEG_PATH)/libavutil/intmath.c \
	$(FFMPEG_PATH)/libavutil/lfg.c \
	$(FFMPEG_PATH)/libavutil/lls.c \
	$(FFMPEG_PATH)/libavutil/log.c \
	$(FFMPEG_PATH)/libavutil/log2_tab.c \
	$(FFMPEG_PATH)/libavutil/mastering_display_metadata.c \
	$(FFMPEG_PATH)/libavutil/mathematics.c \
	$(FFMPEG_PATH)/libavutil/md5.c \
	$(FFMPEG_PATH)/libavutil/mem.c \
	$(FFMPEG_PATH)/libavutil/murmur3.c \
	$(FFMPEG_PATH)/libavutil/opt.c \
	$(FFMPEG_PATH)/libavutil/parseutils.c \
	$(FFMPEG_PATH)/libavutil/pixdesc.c \
	$(FFMPEG_PATH)/libavutil/pixelutils.c \
	$(FFMPEG_PATH)/libavutil/random_seed.c \
	$(FFMPEG_PATH)/libavutil/rational.c \
	$(FFMPEG_PATH)/libavutil/rc4.c \
	$(FFMPEG_PATH)/libavutil/reverse.c \
	$(FFMPEG_PATH)/libavutil/ripemd.c \
	$(FFMPEG_PATH)/libavutil/samplefmt.c \
	$(FFMPEG_PATH)/libavutil/sha.c \
	$(FFMPEG_PATH)/libavutil/sha512.c \
	$(FFMPEG_PATH)/libavutil/stereo3d.c \
	$(FFMPEG_PATH)/libavutil/tea.c \
	$(FFMPEG_PATH)/libavutil/threadmessage.c \
	$(FFMPEG_PATH)/libavutil/time.c \
	$(FFMPEG_PATH)/libavutil/timecode.c \
	$(FFMPEG_PATH)/libavutil/tree.c \
	$(FFMPEG_PATH)/libavutil/twofish.c \
	$(FFMPEG_PATH)/libavutil/utils.c \
	$(FFMPEG_PATH)/libavutil/xga_font_data.c \
	$(FFMPEG_PATH)/libavutil/xtea.c \
	$(FFMPEG_PATH)/compat/strtod.c

local_arm_files := \
	$(FFMPEG_PATH)/libavutil/arm/cpu.c \
	$(FFMPEG_PATH)/libavutil/arm/float_dsp_init_arm.c

local_armv7_files := \
	$(FFMPEG_PATH)/libavutil/arm/cpu.c \
	$(FFMPEG_PATH)/libavutil/arm/float_dsp_init_arm.c \
	$(FFMPEG_PATH)/libavutil/arm/float_dsp_init_neon.c \
	$(FFMPEG_PATH)/libavutil/arm/float_dsp_init_vfp.c \
	$(FFMPEG_PATH)/libavutil/arm/float_dsp_neon.S \
	$(FFMPEG_PATH)/libavutil/arm/float_dsp_vfp.S

local_x86_files := \
	$(FFMPEG_PATH)/libavutil/x86/cpu.c \
	$(FFMPEG_PATH)/libavutil/x86/fixed_dsp_init.c \
	$(FFMPEG_PATH)/libavutil/x86/float_dsp_init.c \
	$(FFMPEG_PATH)/libavutil/x86/lls_init.c

local_c_includes := \
        $(LOCAL_PATH)/$(FFMPEG_PATH) \
	$(LOCAL_PATH)/$(FFMPEG_PATH)/config/$(TARGET_ARCH_ABI)

LOCAL_SRC_FILES += $(local_src_files)
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_SRC_FILES += $(local_armv7_files)
else
    ifeq ($(TARGET_ARCH_ABI),armeabi)
       LOCAL_SRC_FILES += $(local_arm_files)
    else
        ifeq ($(TARGET_ARCH_ABI),x86)
           LOCAL_SRC_FILES += $(local_x86_files)
        endif
    endif
endif

LOCAL_CFLAGS += $(local_c_flags) -DPURIFY -DHAVE_AV_CONFIG_H -D_ISOC99_SOURCE -D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE -Dstrtod=avpriv_strtod -DPIC -DHAVE_AV_CONFIG_H -Os -DANDROID -fPIE -pie --static -std=c99
LOCAL_ARM_MODE:= arm
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= libavutil
include $(BUILD_STATIC_LIBRARY)


################################################################################
# ffmpeg - libavcodec
################################################################################


include $(CLEAR_VARS)


local_src_files := \
	$(FFMPEG_PATH)/libavcodec/ac3tab.c \
	$(FFMPEG_PATH)/libavcodec/allcodecs.c \
	$(FFMPEG_PATH)/libavcodec/avdct.c \
	$(FFMPEG_PATH)/libavcodec/avpicture.c \
	$(FFMPEG_PATH)/libavcodec/audioconvert.c \
	$(FFMPEG_PATH)/libavcodec/bitstream_filter.c \
	$(FFMPEG_PATH)/libavcodec/d3d11va.c \
	$(FFMPEG_PATH)/libavcodec/cabac.c \
	$(FFMPEG_PATH)/libavcodec/codec_desc.c \
	$(FFMPEG_PATH)/libavcodec/dv_profile.c \
	$(FFMPEG_PATH)/libavcodec/dirac.c \
	$(FFMPEG_PATH)/libavcodec/bitstream.c \
	$(FFMPEG_PATH)/libavcodec/fdctdsp.c \
	$(FFMPEG_PATH)/libavcodec/avpacket.c \
	$(FFMPEG_PATH)/libavcodec/golomb.c \
	$(FFMPEG_PATH)/libavcodec/faandct.c \
	$(FFMPEG_PATH)/libavcodec/faanidct.c \
	$(FFMPEG_PATH)/libavcodec/gifdec.c \
	$(FFMPEG_PATH)/libavcodec/h264_direct.c \
	$(FFMPEG_PATH)/libavcodec/error_resilience.c \
	$(FFMPEG_PATH)/libavcodec/h264_picture.c \
	$(FFMPEG_PATH)/libavcodec/h264.c \
	$(FFMPEG_PATH)/libavcodec/h264_ps.c \
	$(FFMPEG_PATH)/libavcodec/h264_sei.c \
	$(FFMPEG_PATH)/libavcodec/h264_refs.c \
	$(FFMPEG_PATH)/libavcodec/h264_cavlc.c \
	$(FFMPEG_PATH)/libavcodec/h264_loopfilter.c \
	$(FFMPEG_PATH)/libavcodec/h264_cabac.c \
	$(FFMPEG_PATH)/libavcodec/h264chroma.c \
	$(FFMPEG_PATH)/libavcodec/h264_mb.c \
	$(FFMPEG_PATH)/libavcodec/h264_slice.c \
	$(FFMPEG_PATH)/libavcodec/h264dsp.c \
	$(FFMPEG_PATH)/libavcodec/h264idct.c \
	$(FFMPEG_PATH)/libavcodec/h264pred.c \
	$(FFMPEG_PATH)/libavcodec/h264qpel.c \
	$(FFMPEG_PATH)/libavcodec/idctdsp.c \
	$(FFMPEG_PATH)/libavcodec/imgconvert.c \
	$(FFMPEG_PATH)/libavcodec/jfdctfst.c \
	$(FFMPEG_PATH)/libavcodec/jfdctint.c \
	$(FFMPEG_PATH)/libavcodec/jrevdct.c \
	$(FFMPEG_PATH)/libavcodec/lzw.c \
	$(FFMPEG_PATH)/libavcodec/mathtables.c \
	$(FFMPEG_PATH)/libavcodec/me_cmp.c \
	$(FFMPEG_PATH)/libavcodec/mpeg4audio.c \
	$(FFMPEG_PATH)/libavcodec/mpegaudiodata.c \
	$(FFMPEG_PATH)/libavcodec/options.c \
	$(FFMPEG_PATH)/libavcodec/parser.c \
	$(FFMPEG_PATH)/libavcodec/pixblockdsp.c \
	$(FFMPEG_PATH)/libavcodec/profiles.c \
	$(FFMPEG_PATH)/libavcodec/pthread.c \
	$(FFMPEG_PATH)/libavcodec/pthread_frame.c \
	$(FFMPEG_PATH)/libavcodec/pthread_slice.c \
	$(FFMPEG_PATH)/libavcodec/qsv_api.c \
	$(FFMPEG_PATH)/libavcodec/raw.c \
	$(FFMPEG_PATH)/libavcodec/resample.c \
	$(FFMPEG_PATH)/libavcodec/resample2.c \
	$(FFMPEG_PATH)/libavcodec/simple_idct.c \
	$(FFMPEG_PATH)/libavcodec/startcode.c \
	$(FFMPEG_PATH)/libavcodec/utils.c \
	$(FFMPEG_PATH)/libavcodec/videodsp.c \
	$(FFMPEG_PATH)/libavcodec/vorbis_parser.c \
	$(FFMPEG_PATH)/libavcodec/xiph.c

local_arm_files := \
	$(FFMPEG_PATH)/libavcodec/arm/h264chroma_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/h264dsp_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/idctdsp_arm.S \
	$(FFMPEG_PATH)/libavcodec/arm/h264qpel_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/jrevdct_arm.S \
	$(FFMPEG_PATH)/libavcodec/arm/h264pred_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/idctdsp_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/simple_idct_arm.S \
	$(FFMPEG_PATH)/libavcodec/arm/simple_idct_armv5te.S \
	$(FFMPEG_PATH)/libavcodec/arm/idctdsp_init_armv5te.c \
	$(FFMPEG_PATH)/libavcodec/arm/me_cmp_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/videodsp_armv5te.S \
	$(FFMPEG_PATH)/libavcodec/arm/pixblockdsp_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/videodsp_init_armv5te.c \
	$(FFMPEG_PATH)/libavcodec/arm/videodsp_init_arm.c

local_armv7_files := \
	$(FFMPEG_PATH)/libavcodec/arm/h264cmc_neon.S \
	$(FFMPEG_PATH)/libavcodec/arm/h264chroma_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/h264dsp_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/h264dsp_neon.S \
	$(FFMPEG_PATH)/libavcodec/arm/h264idct_neon.S \
	$(FFMPEG_PATH)/libavcodec/arm/h264qpel_neon.S \
	$(FFMPEG_PATH)/libavcodec/arm/h264pred_neon.S \
	$(FFMPEG_PATH)/libavcodec/arm/hpeldsp_neon.S \
	$(FFMPEG_PATH)/libavcodec/arm/idctdsp_arm.S \
	$(FFMPEG_PATH)/libavcodec/arm/idctdsp_armv6.S \
	$(FFMPEG_PATH)/libavcodec/arm/h264pred_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/h264qpel_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/idctdsp_neon.S \
	$(FFMPEG_PATH)/libavcodec/arm/idctdsp_init_armv5te.c \
	$(FFMPEG_PATH)/libavcodec/arm/jrevdct_arm.S \
	$(FFMPEG_PATH)/libavcodec/arm/idctdsp_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/idctdsp_init_armv6.c \
	$(FFMPEG_PATH)/libavcodec/arm/me_cmp_armv6.S \
	$(FFMPEG_PATH)/libavcodec/arm/idctdsp_init_neon.c \
	$(FFMPEG_PATH)/libavcodec/arm/pixblockdsp_armv6.S \
	$(FFMPEG_PATH)/libavcodec/arm/simple_idct_arm.S \
	$(FFMPEG_PATH)/libavcodec/arm/simple_idct_armv5te.S \
	$(FFMPEG_PATH)/libavcodec/arm/simple_idct_armv6.S \
	$(FFMPEG_PATH)/libavcodec/arm/simple_idct_neon.S \
	$(FFMPEG_PATH)/libavcodec/arm/startcode_armv6.S \
	$(FFMPEG_PATH)/libavcodec/arm/pixblockdsp_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/me_cmp_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/videodsp_armv5te.S \
	$(FFMPEG_PATH)/libavcodec/arm/videodsp_init_arm.c \
	$(FFMPEG_PATH)/libavcodec/arm/videodsp_init_armv5te.c

local_x86_files := \
	$(FFMPEG_PATH)/libavcodec/x86/constants.c \
	$(FFMPEG_PATH)/libavcodec/x86/fdctdsp_init.c \
	$(FFMPEG_PATH)/libavcodec/x86/h264_intrapred_init.c \
	$(FFMPEG_PATH)/libavcodec/x86/h264chroma_init.c \
	$(FFMPEG_PATH)/libavcodec/x86/h264_qpel.c \
	$(FFMPEG_PATH)/libavcodec/x86/h264dsp_init.c \
	$(FFMPEG_PATH)/libavcodec/x86/idctdsp_init.c \
	$(FFMPEG_PATH)/libavcodec/x86/me_cmp_init.c \
	$(FFMPEG_PATH)/libavcodec/x86/pixblockdsp_init.c \
	$(FFMPEG_PATH)/libavcodec/x86/videodsp_init.c

local_c_includes := \
        $(LOCAL_PATH)/$(FFMPEG_PATH) \
	$(LOCAL_PATH)/$(FFMPEG_PATH)/config/$(TARGET_ARCH_ABI)

LOCAL_SRC_FILES += $(local_src_files)
ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_SRC_FILES += $(local_armv7_files)
else
    ifeq ($(TARGET_ARCH_ABI),armeabi)
       LOCAL_SRC_FILES += $(local_arm_files)
    else
        ifeq ($(TARGET_ARCH_ABI),x86)
           LOCAL_SRC_FILES += $(local_x86_files)
        endif
    endif
endif

LOCAL_CFLAGS += $(local_c_flags) -DPURIFY -DHAVE_AV_CONFIG_H -D_ISOC99_SOURCE -D_FILE_OFFSET_BITS=64 -D_LARGEFILE_SOURCE -Dstrtod=avpriv_strtod -DPIC -DHAVE_AV_CONFIG_H -Os -DANDROID -fPIE -pie --static -std=c99
LOCAL_ARM_MODE:= arm
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= libavcodec
include $(BUILD_STATIC_LIBRARY)

################################################################################
# openssl - libcrypto
################################################################################

include $(CLEAR_VARS)


CRYPTO_PATH:= ./openssl/crypto

arm_cflags := -DOPENSSL_BN_ASM_MONT -DAES_ASM -DSHA1_ASM -DSHA256_ASM -DSHA512_ASM

local_src_files := \
	./openssl/crypto/cryptlib.c \
	./openssl/crypto/mem.c \
	./openssl/crypto/mem_clr.c \
	./openssl/crypto/mem_dbg.c \
	./openssl/crypto/cversion.c \
	./openssl/crypto/ex_data.c \
	./openssl/crypto/cpt_err.c \
	./openssl/crypto/ebcdic.c \
	./openssl/crypto/uid.c \
	./openssl/crypto/o_time.c \
	./openssl/crypto/o_str.c \
	./openssl/crypto/o_dir.c \
	./openssl/crypto/o_init.c \
	./openssl/crypto/aes/aes_cbc.c \
	./openssl/crypto/aes/aes_core.c \
	./openssl/crypto/aes/aes_cfb.c \
	./openssl/crypto/aes/aes_ctr.c \
	./openssl/crypto/aes/aes_ecb.c \
	./openssl/crypto/aes/aes_ige.c \
	./openssl/crypto/aes/aes_misc.c \
	./openssl/crypto/aes/aes_ofb.c \
	./openssl/crypto/aes/aes_wrap.c \
	./openssl/crypto/asn1/a_bitstr.c \
	./openssl/crypto/asn1/a_bool.c \
	./openssl/crypto/asn1/a_bytes.c \
	./openssl/crypto/asn1/a_d2i_fp.c \
	./openssl/crypto/asn1/a_digest.c \
	./openssl/crypto/asn1/a_dup.c \
	./openssl/crypto/asn1/a_enum.c \
	./openssl/crypto/asn1/a_gentm.c \
	./openssl/crypto/asn1/a_i2d_fp.c \
	./openssl/crypto/asn1/a_int.c \
	./openssl/crypto/asn1/a_mbstr.c \
	./openssl/crypto/asn1/a_object.c \
	./openssl/crypto/asn1/a_octet.c \
	./openssl/crypto/asn1/a_print.c \
	./openssl/crypto/asn1/a_set.c \
	./openssl/crypto/asn1/a_sign.c \
	./openssl/crypto/asn1/a_strex.c \
	./openssl/crypto/asn1/a_strnid.c \
	./openssl/crypto/asn1/a_time.c \
	./openssl/crypto/asn1/a_type.c \
	./openssl/crypto/asn1/a_utctm.c \
	./openssl/crypto/asn1/a_utf8.c \
	./openssl/crypto/asn1/a_verify.c \
	./openssl/crypto/asn1/ameth_lib.c \
	./openssl/crypto/asn1/asn1_err.c \
	./openssl/crypto/asn1/asn1_gen.c \
	./openssl/crypto/asn1/asn1_lib.c \
	./openssl/crypto/asn1/asn1_par.c \
	./openssl/crypto/asn1/asn_mime.c \
	./openssl/crypto/asn1/asn_moid.c \
	./openssl/crypto/asn1/asn_pack.c \
	./openssl/crypto/asn1/bio_asn1.c \
	./openssl/crypto/asn1/bio_ndef.c \
	./openssl/crypto/asn1/d2i_pr.c \
	./openssl/crypto/asn1/d2i_pu.c \
	./openssl/crypto/asn1/evp_asn1.c \
	./openssl/crypto/asn1/f_enum.c \
	./openssl/crypto/asn1/f_int.c \
	./openssl/crypto/asn1/f_string.c \
	./openssl/crypto/asn1/i2d_pr.c \
	./openssl/crypto/asn1/i2d_pu.c \
	./openssl/crypto/asn1/n_pkey.c \
	./openssl/crypto/asn1/nsseq.c \
	./openssl/crypto/asn1/p5_pbe.c \
	./openssl/crypto/asn1/p5_pbev2.c \
	./openssl/crypto/asn1/p8_pkey.c \
	./openssl/crypto/asn1/t_bitst.c \
	./openssl/crypto/asn1/t_crl.c \
	./openssl/crypto/asn1/t_pkey.c \
	./openssl/crypto/asn1/t_req.c \
	./openssl/crypto/asn1/t_spki.c \
	./openssl/crypto/asn1/t_x509.c \
	./openssl/crypto/asn1/t_x509a.c \
	./openssl/crypto/asn1/tasn_dec.c \
	./openssl/crypto/asn1/tasn_enc.c \
	./openssl/crypto/asn1/tasn_fre.c \
	./openssl/crypto/asn1/tasn_new.c \
	./openssl/crypto/asn1/tasn_prn.c \
	./openssl/crypto/asn1/tasn_typ.c \
	./openssl/crypto/asn1/tasn_utl.c \
	./openssl/crypto/asn1/x_algor.c \
	./openssl/crypto/asn1/x_attrib.c \
	./openssl/crypto/asn1/x_bignum.c \
	./openssl/crypto/asn1/x_crl.c \
	./openssl/crypto/asn1/x_exten.c \
	./openssl/crypto/asn1/x_info.c \
	./openssl/crypto/asn1/x_long.c \
	./openssl/crypto/asn1/x_name.c \
	./openssl/crypto/asn1/x_nx509.c \
	./openssl/crypto/asn1/x_pkey.c \
	./openssl/crypto/asn1/x_pubkey.c \
	./openssl/crypto/asn1/x_req.c \
	./openssl/crypto/asn1/x_sig.c \
	./openssl/crypto/asn1/x_spki.c \
	./openssl/crypto/asn1/x_val.c \
	./openssl/crypto/asn1/x_x509.c \
	./openssl/crypto/asn1/x_x509a.c \
	./openssl/crypto/bf/bf_cfb64.c \
	./openssl/crypto/bf/bf_ecb.c \
	./openssl/crypto/bf/bf_enc.c \
	./openssl/crypto/bf/bf_ofb64.c \
	./openssl/crypto/bf/bf_skey.c \
	./openssl/crypto/bio/b_dump.c \
	./openssl/crypto/bio/b_print.c \
	./openssl/crypto/bio/b_sock.c \
	./openssl/crypto/bio/bf_buff.c \
	./openssl/crypto/bio/bf_nbio.c \
	./openssl/crypto/bio/bf_null.c \
	./openssl/crypto/bio/bio_cb.c \
	./openssl/crypto/bio/bio_err.c \
	./openssl/crypto/bio/bio_lib.c \
	./openssl/crypto/bio/bss_acpt.c \
	./openssl/crypto/bio/bss_bio.c \
	./openssl/crypto/bio/bss_conn.c \
	./openssl/crypto/bio/bss_dgram.c \
	./openssl/crypto/bio/bss_fd.c \
	./openssl/crypto/bio/bss_file.c \
	./openssl/crypto/bio/bss_log.c \
	./openssl/crypto/bio/bss_mem.c \
	./openssl/crypto/bio/bss_null.c \
	./openssl/crypto/bio/bss_sock.c \
	./openssl/crypto/bn/bn_add.c \
	./openssl/crypto/bn/bn_asm.c \
	./openssl/crypto/bn/bn_blind.c \
	./openssl/crypto/bn/bn_ctx.c \
	./openssl/crypto/bn/bn_div.c \
	./openssl/crypto/bn/bn_err.c \
	./openssl/crypto/bn/bn_exp.c \
	./openssl/crypto/bn/bn_exp2.c \
	./openssl/crypto/bn/bn_gcd.c \
	./openssl/crypto/bn/bn_gf2m.c \
	./openssl/crypto/bn/bn_kron.c \
	./openssl/crypto/bn/bn_lib.c \
	./openssl/crypto/bn/bn_mod.c \
	./openssl/crypto/bn/bn_mont.c \
	./openssl/crypto/bn/bn_mpi.c \
	./openssl/crypto/bn/bn_mul.c \
	./openssl/crypto/bn/bn_nist.c \
	./openssl/crypto/bn/bn_prime.c \
	./openssl/crypto/bn/bn_print.c \
	./openssl/crypto/bn/bn_rand.c \
	./openssl/crypto/bn/bn_recp.c \
	./openssl/crypto/bn/bn_shift.c \
	./openssl/crypto/bn/bn_sqr.c \
	./openssl/crypto/bn/bn_sqrt.c \
	./openssl/crypto/bn/bn_word.c \
	./openssl/crypto/buffer/buf_err.c \
	./openssl/crypto/buffer/buf_str.c \
	./openssl/crypto/buffer/buffer.c \
	./openssl/crypto/camellia/camellia.c \
	./openssl/crypto/camellia/cmll_cbc.c \
	./openssl/crypto/camellia/cmll_cfb.c \
	./openssl/crypto/camellia/cmll_ctr.c \
	./openssl/crypto/camellia/cmll_ecb.c \
	./openssl/crypto/camellia/cmll_misc.c \
	./openssl/crypto/camellia/cmll_ofb.c \
	./openssl/crypto/camellia/cmll_utl.c \
	./openssl/crypto/cast/c_cfb64.c \
	./openssl/crypto/cast/c_ecb.c \
	./openssl/crypto/cast/c_enc.c \
	./openssl/crypto/cast/c_ofb64.c \
	./openssl/crypto/cast/c_skey.c \
	./openssl/crypto/cmac/cm_ameth.c \
	./openssl/crypto/cmac/cm_pmeth.c \
	./openssl/crypto/cmac/cmac.c \
	./openssl/crypto/comp/c_rle.c \
	./openssl/crypto/comp/c_zlib.c \
	./openssl/crypto/comp/comp_err.c \
	./openssl/crypto/comp/comp_lib.c \
	./openssl/crypto/conf/conf_api.c \
	./openssl/crypto/conf/conf_def.c \
	./openssl/crypto/conf/conf_err.c \
	./openssl/crypto/conf/conf_lib.c \
	./openssl/crypto/conf/conf_mall.c \
	./openssl/crypto/conf/conf_mod.c \
	./openssl/crypto/conf/conf_sap.c \
	./openssl/crypto/des/cbc_cksm.c \
	./openssl/crypto/des/cbc_enc.c \
	./openssl/crypto/des/cfb64ede.c \
	./openssl/crypto/des/cfb64enc.c \
	./openssl/crypto/des/cfb_enc.c \
	./openssl/crypto/des/des_enc.c \
	./openssl/crypto/des/des_old.c \
	./openssl/crypto/des/des_old2.c \
	./openssl/crypto/des/ecb3_enc.c \
	./openssl/crypto/des/ecb_enc.c \
	./openssl/crypto/des/ede_cbcm_enc.c \
	./openssl/crypto/des/enc_read.c \
	./openssl/crypto/des/enc_writ.c \
	./openssl/crypto/des/fcrypt.c \
	./openssl/crypto/des/fcrypt_b.c \
	./openssl/crypto/des/ofb64ede.c \
	./openssl/crypto/des/ofb64enc.c \
	./openssl/crypto/des/ofb_enc.c \
	./openssl/crypto/des/pcbc_enc.c \
	./openssl/crypto/des/qud_cksm.c \
	./openssl/crypto/des/rand_key.c \
	./openssl/crypto/des/read2pwd.c \
	./openssl/crypto/des/rpc_enc.c \
	./openssl/crypto/des/set_key.c \
	./openssl/crypto/des/str2key.c \
	./openssl/crypto/des/xcbc_enc.c \
	./openssl/crypto/dh/dh_ameth.c \
	./openssl/crypto/dh/dh_asn1.c \
	./openssl/crypto/dh/dh_check.c \
	./openssl/crypto/dh/dh_depr.c \
	./openssl/crypto/dh/dh_err.c \
	./openssl/crypto/dh/dh_gen.c \
	./openssl/crypto/dh/dh_key.c \
	./openssl/crypto/dh/dh_lib.c \
	./openssl/crypto/dh/dh_pmeth.c \
	./openssl/crypto/dsa/dsa_ameth.c \
	./openssl/crypto/dsa/dsa_asn1.c \
	./openssl/crypto/dsa/dsa_depr.c \
	./openssl/crypto/dsa/dsa_err.c \
	./openssl/crypto/dsa/dsa_gen.c \
	./openssl/crypto/dsa/dsa_key.c \
	./openssl/crypto/dsa/dsa_lib.c \
	./openssl/crypto/dsa/dsa_ossl.c \
	./openssl/crypto/dsa/dsa_pmeth.c \
	./openssl/crypto/dsa/dsa_prn.c \
	./openssl/crypto/dsa/dsa_sign.c \
	./openssl/crypto/dsa/dsa_vrf.c \
	./openssl/crypto/dso/dso_dl.c \
	./openssl/crypto/dso/dso_dlfcn.c \
	./openssl/crypto/dso/dso_err.c \
	./openssl/crypto/dso/dso_lib.c \
	./openssl/crypto/dso/dso_null.c \
	./openssl/crypto/dso/dso_openssl.c \
	./openssl/crypto/dso/dso_vms.c \
	./openssl/crypto/dso/dso_win32.c \
	./openssl/crypto/ec/ec2_mult.c \
	./openssl/crypto/ec/ec2_oct.c \
	./openssl/crypto/ec/ec2_smpl.c \
	./openssl/crypto/ec/ec_ameth.c \
	./openssl/crypto/ec/ec_asn1.c \
	./openssl/crypto/ec/ec_check.c \
	./openssl/crypto/ec/ec_curve.c \
	./openssl/crypto/ec/ec_cvt.c \
	./openssl/crypto/ec/ec_err.c \
	./openssl/crypto/ec/ec_key.c \
	./openssl/crypto/ec/ec_lib.c \
	./openssl/crypto/ec/ec_mult.c \
	./openssl/crypto/ec/ec_oct.c \
	./openssl/crypto/ec/ec_pmeth.c \
	./openssl/crypto/ec/ec_print.c \
	./openssl/crypto/ec/eck_prn.c \
	./openssl/crypto/ec/ecp_mont.c \
	./openssl/crypto/ec/ecp_nist.c \
	./openssl/crypto/ec/ecp_nistp224.c \
	./openssl/crypto/ec/ecp_nistp256.c \
	./openssl/crypto/ec/ecp_nistp521.c \
	./openssl/crypto/ec/ecp_nistputil.c \
	./openssl/crypto/ec/ecp_oct.c \
	./openssl/crypto/ec/ecp_smpl.c \
	./openssl/crypto/ecdh/ech_err.c \
	./openssl/crypto/ecdh/ech_key.c \
	./openssl/crypto/ecdh/ech_lib.c \
	./openssl/crypto/ecdh/ech_ossl.c \
	./openssl/crypto/ecdsa/ecs_asn1.c \
	./openssl/crypto/ecdsa/ecs_err.c \
	./openssl/crypto/ecdsa/ecs_lib.c \
	./openssl/crypto/ecdsa/ecs_ossl.c \
	./openssl/crypto/ecdsa/ecs_sign.c \
	./openssl/crypto/ecdsa/ecs_vrf.c \
	./openssl/crypto/err/err.c \
	./openssl/crypto/err/err_all.c \
	./openssl/crypto/err/err_prn.c \
	./openssl/crypto/evp/bio_b64.c \
	./openssl/crypto/evp/bio_enc.c \
	./openssl/crypto/evp/bio_md.c \
	./openssl/crypto/evp/bio_ok.c \
	./openssl/crypto/evp/c_all.c \
	./openssl/crypto/evp/c_allc.c \
	./openssl/crypto/evp/c_alld.c \
	./openssl/crypto/evp/digest.c \
	./openssl/crypto/evp/e_aes.c \
	./openssl/crypto/evp/e_aes_cbc_hmac_sha1.c \
	./openssl/crypto/evp/e_bf.c \
	./openssl/crypto/evp/e_camellia.c \
	./openssl/crypto/evp/e_cast.c \
	./openssl/crypto/evp/e_des.c \
	./openssl/crypto/evp/e_des3.c \
	./openssl/crypto/evp/e_null.c \
	./openssl/crypto/evp/e_old.c \
	./openssl/crypto/evp/e_rc2.c \
	./openssl/crypto/evp/e_rc4.c \
	./openssl/crypto/evp/e_rc4_hmac_md5.c \
	./openssl/crypto/evp/e_rc5.c \
	./openssl/crypto/evp/e_xcbc_d.c \
	./openssl/crypto/evp/encode.c \
	./openssl/crypto/evp/evp_acnf.c \
	./openssl/crypto/evp/evp_enc.c \
	./openssl/crypto/evp/evp_err.c \
	./openssl/crypto/evp/evp_key.c \
	./openssl/crypto/evp/evp_lib.c \
	./openssl/crypto/evp/evp_pbe.c \
	./openssl/crypto/evp/evp_pkey.c \
	./openssl/crypto/evp/m_dss.c \
	./openssl/crypto/evp/m_dss1.c \
	./openssl/crypto/evp/m_ecdsa.c \
	./openssl/crypto/evp/m_md4.c \
	./openssl/crypto/evp/m_md5.c \
	./openssl/crypto/evp/m_mdc2.c \
	./openssl/crypto/evp/m_null.c \
	./openssl/crypto/evp/m_ripemd.c \
	./openssl/crypto/evp/m_sha1.c \
	./openssl/crypto/evp/m_sigver.c \
	./openssl/crypto/evp/m_wp.c \
	./openssl/crypto/evp/names.c \
	./openssl/crypto/evp/p5_crpt.c \
	./openssl/crypto/evp/p5_crpt2.c \
	./openssl/crypto/evp/p_dec.c \
	./openssl/crypto/evp/p_enc.c \
	./openssl/crypto/evp/p_lib.c \
	./openssl/crypto/evp/p_open.c \
	./openssl/crypto/evp/p_seal.c \
	./openssl/crypto/evp/p_sign.c \
	./openssl/crypto/evp/p_verify.c \
	./openssl/crypto/evp/pmeth_fn.c \
	./openssl/crypto/evp/pmeth_gn.c \
	./openssl/crypto/evp/pmeth_lib.c \
	./openssl/crypto/hmac/hm_ameth.c \
	./openssl/crypto/hmac/hm_pmeth.c \
	./openssl/crypto/hmac/hmac.c \
	./openssl/crypto/krb5/krb5_asn.c \
	./openssl/crypto/lhash/lh_stats.c \
	./openssl/crypto/lhash/lhash.c \
	./openssl/crypto/md4/md4_dgst.c \
	./openssl/crypto/md4/md4_one.c \
	./openssl/crypto/md5/md5_dgst.c \
	./openssl/crypto/md5/md5_one.c \
	./openssl/crypto/modes/cbc128.c \
	./openssl/crypto/modes/ccm128.c \
	./openssl/crypto/modes/cfb128.c \
	./openssl/crypto/modes/ctr128.c \
	./openssl/crypto/modes/cts128.c \
	./openssl/crypto/modes/gcm128.c \
	./openssl/crypto/modes/ofb128.c \
	./openssl/crypto/modes/xts128.c \
	./openssl/crypto/objects/o_names.c \
	./openssl/crypto/objects/obj_dat.c \
	./openssl/crypto/objects/obj_err.c \
	./openssl/crypto/objects/obj_lib.c \
	./openssl/crypto/objects/obj_xref.c \
	./openssl/crypto/ocsp/ocsp_asn.c \
	./openssl/crypto/ocsp/ocsp_cl.c \
	./openssl/crypto/ocsp/ocsp_err.c \
	./openssl/crypto/ocsp/ocsp_ext.c \
	./openssl/crypto/ocsp/ocsp_ht.c \
	./openssl/crypto/ocsp/ocsp_lib.c \
	./openssl/crypto/ocsp/ocsp_prn.c \
	./openssl/crypto/ocsp/ocsp_srv.c \
	./openssl/crypto/ocsp/ocsp_vfy.c \
	./openssl/crypto/pem/pem_all.c \
	./openssl/crypto/pem/pem_err.c \
	./openssl/crypto/pem/pem_info.c \
	./openssl/crypto/pem/pem_lib.c \
	./openssl/crypto/pem/pem_oth.c \
	./openssl/crypto/pem/pem_pk8.c \
	./openssl/crypto/pem/pem_pkey.c \
	./openssl/crypto/pem/pem_seal.c \
	./openssl/crypto/pem/pem_sign.c \
	./openssl/crypto/pem/pem_x509.c \
	./openssl/crypto/pem/pem_xaux.c \
	./openssl/crypto/pem/pvkfmt.c \
	./openssl/crypto/pkcs12/p12_add.c \
	./openssl/crypto/pkcs12/p12_asn.c \
	./openssl/crypto/pkcs12/p12_attr.c \
	./openssl/crypto/pkcs12/p12_crpt.c \
	./openssl/crypto/pkcs12/p12_crt.c \
	./openssl/crypto/pkcs12/p12_decr.c \
	./openssl/crypto/pkcs12/p12_init.c \
	./openssl/crypto/pkcs12/p12_key.c \
	./openssl/crypto/pkcs12/p12_kiss.c \
	./openssl/crypto/pkcs12/p12_mutl.c \
	./openssl/crypto/pkcs12/p12_npas.c \
	./openssl/crypto/pkcs12/p12_p8d.c \
	./openssl/crypto/pkcs12/p12_p8e.c \
	./openssl/crypto/pkcs12/p12_utl.c \
	./openssl/crypto/pkcs12/pk12err.c \
	./openssl/crypto/pkcs7/pk7_asn1.c \
	./openssl/crypto/pkcs7/pk7_attr.c \
	./openssl/crypto/pkcs7/pk7_doit.c \
	./openssl/crypto/pkcs7/pk7_lib.c \
	./openssl/crypto/pkcs7/pk7_mime.c \
	./openssl/crypto/pkcs7/pk7_smime.c \
	./openssl/crypto/pkcs7/pkcs7err.c \
	./openssl/crypto/pqueue/pqueue.c \
	./openssl/crypto/rand/md_rand.c \
	./openssl/crypto/rand/rand_egd.c \
	./openssl/crypto/rand/rand_err.c \
	./openssl/crypto/rand/rand_lib.c \
	./openssl/crypto/rand/rand_unix.c \
	./openssl/crypto/rand/randfile.c \
	./openssl/crypto/rc2/rc2_cbc.c \
	./openssl/crypto/rc2/rc2_ecb.c \
	./openssl/crypto/rc2/rc2_skey.c \
	./openssl/crypto/rc2/rc2cfb64.c \
	./openssl/crypto/rc2/rc2ofb64.c \
	./openssl/crypto/rc4/rc4_enc.c \
	./openssl/crypto/rc4/rc4_skey.c \
	./openssl/crypto/rc4/rc4_utl.c \
	./openssl/crypto/ripemd/rmd_dgst.c \
	./openssl/crypto/ripemd/rmd_one.c \
	./openssl/crypto/rsa/rsa_ameth.c \
	./openssl/crypto/rsa/rsa_asn1.c \
	./openssl/crypto/rsa/rsa_chk.c \
	./openssl/crypto/rsa/rsa_crpt.c \
	./openssl/crypto/rsa/rsa_depr.c \
	./openssl/crypto/rsa/rsa_eay.c \
	./openssl/crypto/rsa/rsa_err.c \
	./openssl/crypto/rsa/rsa_gen.c \
	./openssl/crypto/rsa/rsa_lib.c \
	./openssl/crypto/rsa/rsa_none.c \
	./openssl/crypto/rsa/rsa_null.c \
	./openssl/crypto/rsa/rsa_oaep.c \
	./openssl/crypto/rsa/rsa_pk1.c \
	./openssl/crypto/rsa/rsa_pmeth.c \
	./openssl/crypto/rsa/rsa_prn.c \
	./openssl/crypto/rsa/rsa_pss.c \
	./openssl/crypto/rsa/rsa_saos.c \
	./openssl/crypto/rsa/rsa_sign.c \
	./openssl/crypto/rsa/rsa_ssl.c \
	./openssl/crypto/rsa/rsa_x931.c \
	./openssl/crypto/sha/sha1_one.c \
	./openssl/crypto/sha/sha1dgst.c \
	./openssl/crypto/sha/sha256.c \
	./openssl/crypto/sha/sha512.c \
	./openssl/crypto/sha/sha_dgst.c \
	./openssl/crypto/srp/srp_lib.c \
	./openssl/crypto/srp/srp_vfy.c \
	./openssl/crypto/stack/stack.c \
	./openssl/crypto/ts/ts_err.c \
	./openssl/crypto/txt_db/txt_db.c \
	./openssl/crypto/ui/ui_compat.c \
	./openssl/crypto/ui/ui_err.c \
	./openssl/crypto/ui/ui_lib.c \
	./openssl/crypto/ui/ui_openssl.c \
	./openssl/crypto/ui/ui_util.c \
	./openssl/crypto/x509/by_dir.c \
	./openssl/crypto/x509/by_file.c \
	./openssl/crypto/x509/x509_att.c \
	./openssl/crypto/x509/x509_cmp.c \
	./openssl/crypto/x509/x509_d2.c \
	./openssl/crypto/x509/x509_def.c \
	./openssl/crypto/x509/x509_err.c \
	./openssl/crypto/x509/x509_ext.c \
	./openssl/crypto/x509/x509_lu.c \
	./openssl/crypto/x509/x509_obj.c \
	./openssl/crypto/x509/x509_r2x.c \
	./openssl/crypto/x509/x509_req.c \
	./openssl/crypto/x509/x509_set.c \
	./openssl/crypto/x509/x509_trs.c \
	./openssl/crypto/x509/x509_txt.c \
	./openssl/crypto/x509/x509_v3.c \
	./openssl/crypto/x509/x509_vfy.c \
	./openssl/crypto/x509/x509_vpm.c \
	./openssl/crypto/x509/x509cset.c \
	./openssl/crypto/x509/x509name.c \
	./openssl/crypto/x509/x509rset.c \
	./openssl/crypto/x509/x509spki.c \
	./openssl/crypto/x509/x509type.c \
	./openssl/crypto/x509/x_all.c \
	./openssl/crypto/x509v3/pcy_cache.c \
	./openssl/crypto/x509v3/pcy_data.c \
	./openssl/crypto/x509v3/pcy_lib.c \
	./openssl/crypto/x509v3/pcy_map.c \
	./openssl/crypto/x509v3/pcy_node.c \
	./openssl/crypto/x509v3/pcy_tree.c \
	./openssl/crypto/x509v3/v3_akey.c \
	./openssl/crypto/x509v3/v3_akeya.c \
	./openssl/crypto/x509v3/v3_alt.c \
	./openssl/crypto/x509v3/v3_bcons.c \
	./openssl/crypto/x509v3/v3_bitst.c \
	./openssl/crypto/x509v3/v3_conf.c \
	./openssl/crypto/x509v3/v3_cpols.c \
	./openssl/crypto/x509v3/v3_crld.c \
	./openssl/crypto/x509v3/v3_enum.c \
	./openssl/crypto/x509v3/v3_extku.c \
	./openssl/crypto/x509v3/v3_genn.c \
	./openssl/crypto/x509v3/v3_ia5.c \
	./openssl/crypto/x509v3/v3_info.c \
	./openssl/crypto/x509v3/v3_int.c \
	./openssl/crypto/x509v3/v3_lib.c \
	./openssl/crypto/x509v3/v3_ncons.c \
	./openssl/crypto/x509v3/v3_ocsp.c \
	./openssl/crypto/x509v3/v3_pci.c \
	./openssl/crypto/x509v3/v3_pcia.c \
	./openssl/crypto/x509v3/v3_pcons.c \
	./openssl/crypto/x509v3/v3_pku.c \
	./openssl/crypto/x509v3/v3_pmaps.c \
	./openssl/crypto/x509v3/v3_prn.c \
	./openssl/crypto/x509v3/v3_purp.c \
	./openssl/crypto/x509v3/v3_skey.c \
	./openssl/crypto/x509v3/v3_sxnet.c \
	./openssl/crypto/x509v3/v3_utl.c \
	./openssl/crypto/x509v3/v3err.c \
	./openssl/ssl/bio_ssl.c   \
	./openssl/ssl/s2_meth.c  \
	./openssl/ssl/ssl_algs.c  \
	./openssl/ssl/kssl.c      \
	./openssl/ssl/s2_pkt.c   \
	./openssl/ssl/ssl_asn1.c  \
	./openssl/ssl/d1_both.c   \
	./openssl/ssl/s2_srvr.c  \
	./openssl/ssl/ssl_cert.c  \
	./openssl/ssl/ssl_txt.c \
	./openssl/ssl/d1_clnt.c   \
	./openssl/ssl/s23_clnt.c  \
	./openssl/ssl/s3_both.c  \
	./openssl/ssl/ssl_ciph.c  \
	./openssl/ssl/ssl_utst.c \
	./openssl/ssl/d1_enc.c    \
	./openssl/ssl/s23_lib.c   \
	./openssl/ssl/s3_cbc.c   \
	./openssl/ssl/ssl_err2.c  \
	./openssl/ssl/t1_clnt.c \
	./openssl/ssl/d1_lib.c    \
	./openssl/ssl/s23_meth.c  \
	./openssl/ssl/s3_clnt.c  \
	./openssl/ssl/ssl_err.c   \
	./openssl/ssl/t1_enc.c \
	./openssl/ssl/d1_meth.c   \
	./openssl/ssl/s23_pkt.c   \
	./openssl/ssl/s3_enc.c   \
	./openssl/ssl/ssl_lib.c   \
	./openssl/ssl/t1_lib.c \
	./openssl/ssl/d1_pkt.c    \
	./openssl/ssl/s23_srvr.c  \
	./openssl/ssl/s3_lib.c   \
	./openssl/ssl/t1_meth.c \
	./openssl/ssl/d1_srtp.c   \
	./openssl/ssl/s2_clnt.c   \
	./openssl/ssl/s3_meth.c  \
	./openssl/ssl/ssl_rsa.c   \
	./openssl/ssl/t1_reneg.c \
	./openssl/ssl/d1_srvr.c   \
	./openssl/ssl/s2_enc.c    \
	./openssl/ssl/s3_pkt.c   \
	./openssl/ssl/ssl_sess.c  \
	./openssl/ssl/t1_srvr.c \
	./openssl/ssl/s2_lib.c    \
	./openssl/ssl/s3_srvr.c  \
	./openssl/ssl/ssl_stat.c  \
	./openssl/ssl/tls_srp.c

local_c_includes := \
	$(LOCAL_PATH)/openssl \
	$(LOCAL_PATH)/openssl/crypto \
	$(LOCAL_PATH)/openssl/crypto/asn1 \
	$(LOCAL_PATH)/openssl/crypto/evp \
	$(LOCAL_PATH)/openssl/crypto/modes \
	$(LOCAL_PATH)/openssl/include \
	$(LOCAL_PATH)/openssl/include/openssl

local_c_flags := -DNO_WINDOWS_BRAINDEATH


include $(LOCAL_PATH)/openssl/android-config.mk
LOCAL_SRC_FILES += $(local_src_files)
LOCAL_CFLAGS += $(local_c_flags) -DPURIFY
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= libcrypto
include $(BUILD_STATIC_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE    := crypto
#
#ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
#    LOCAL_SRC_FILES := ./openssl/obj/local/armeabi-v7a/libcrypto.a
#else
#    ifeq ($(TARGET_ARCH_ABI),armeabi)
#       LOCAL_SRC_FILES := ./openssl/obj/local/armeabi/libcrypto.a
#    else
#        ifeq ($(TARGET_ARCH_ABI),x86)
#           LOCAL_SRC_FILES := ./openssl/obj/local/x86/libcrypto.a
#        endif
#    endif
#endif

#include $(PREBUILT_STATIC_LIBRARY)


################################################################################
# breakpad
################################################################################


#include $(CLEAR_VARS)
#
#LOCAL_CPP_EXTENSION := .cc
#LOCAL_ARM_MODE := arm
#LOCAL_MODULE := breakpad
#LOCAL_CPPFLAGS := -Wall -std=c++11 -DANDROID -finline-functions -ffast-math -Os -fno-strict-aliasing
#
#LOCAL_C_INCLUDES := \
#$(LOCAL_PATH)/breakpad/common/android/include \
#$(LOCAL_PATH)/breakpad
#
#LOCAL_SRC_FILES := \
#./breakpad/client/linux/crash_generation/crash_generation_client.cc \
#./breakpad/client/linux/dump_writer_common/ucontext_reader.cc \
#./breakpad/client/linux/dump_writer_common/thread_info.cc \
#./breakpad/client/linux/handler/exception_handler.cc \
#./breakpad/client/linux/handler/minidump_descriptor.cc \
#./breakpad/client/linux/log/log.cc \
#./breakpad/client/linux/microdump_writer/microdump_writer.cc \
#./breakpad/client/linux/minidump_writer/linux_dumper.cc \
#./breakpad/client/linux/minidump_writer/linux_ptrace_dumper.cc \
#./breakpad/client/linux/minidump_writer/minidump_writer.cc \
#./breakpad/client/minidump_file_writer.cc \
#./breakpad/common/android/breakpad_getcontext.S \
#./breakpad/common/convert_UTF.c \
#./breakpad/common/md5.cc \
#./breakpad/common/string_conversion.cc \
#./breakpad/common/linux/elfutils.cc \
#./breakpad/common/linux/file_id.cc \
#./breakpad/common/linux/guid_creator.cc \
#./breakpad/common/linux/linux_libc_support.cc \
#./breakpad/common/linux/memory_mapped_file.cc \
#./breakpad/common/linux/safe_readlink.cc
#
#include $(BUILD_STATIC_LIBRARY)


################################################################################
# webp
################################################################################


#include $(CLEAR_VARS)
#
#LOCAL_CFLAGS := -Wall -DANDROID -DHAVE_MALLOC_H -DHAVE_PTHREAD -DWEBP_USE_THREAD -finline-functions -ffast-math -ffunction-sections -fdata-sections -Os
#LOCAL_C_INCLUDES += $(LOCAL_PATH)/libwebp/src
#LOCAL_ARM_MODE := arm
#LOCAL_STATIC_LIBRARIES := cpufeatures
#LOCAL_MODULE := webp
#
#ifneq ($(findstring armeabi-v7a, $(TARGET_ARCH_ABI)),)
#  NEON := c.neon
#else
#  NEON := c
#endif
#
#LOCAL_SRC_FILES := \
#./libwebp/dec/alpha.c \
#./libwebp/dec/buffer.c \
#./libwebp/dec/frame.c \
#./libwebp/dec/idec.c \
#./libwebp/dec/io.c \
#./libwebp/dec/quant.c \
#./libwebp/dec/tree.c \
#./libwebp/dec/vp8.c \
#./libwebp/dec/vp8l.c \
#./libwebp/dec/webp.c \
#./libwebp/dsp/alpha_processing.c \
#./libwebp/dsp/alpha_processing_sse2.c \
#./libwebp/dsp/cpu.c \
#./libwebp/dsp/dec.c \
#./libwebp/dsp/dec_clip_tables.c \
#./libwebp/dsp/dec_mips32.c \
#./libwebp/dsp/dec_neon.$(NEON) \
#./libwebp/dsp/dec_sse2.c \
#./libwebp/dsp/enc.c \
#./libwebp/dsp/enc_avx2.c \
#./libwebp/dsp/enc_mips32.c \
#./libwebp/dsp/enc_neon.$(NEON) \
#./libwebp/dsp/enc_sse2.c \
#./libwebp/dsp/lossless.c \
#./libwebp/dsp/lossless_mips32.c \
#./libwebp/dsp/lossless_neon.$(NEON) \
#./libwebp/dsp/lossless_sse2.c \
#./libwebp/dsp/upsampling.c \
#./libwebp/dsp/upsampling_neon.$(NEON) \
#./libwebp/dsp/upsampling_sse2.c \
#./libwebp/dsp/yuv.c \
#./libwebp/dsp/yuv_mips32.c \
#./libwebp/dsp/yuv_sse2.c \
#./libwebp/enc/alpha.c \
#./libwebp/enc/analysis.c \
#./libwebp/enc/backward_references.c \
#./libwebp/enc/config.c \
#./libwebp/enc/cost.c \
#./libwebp/enc/filter.c \
#./libwebp/enc/frame.c \
#./libwebp/enc/histogram.c \
#./libwebp/enc/iterator.c \
#./libwebp/enc/picture.c \
#./libwebp/enc/picture_csp.c \
#./libwebp/enc/picture_psnr.c \
#./libwebp/enc/picture_rescale.c \
#./libwebp/enc/picture_tools.c \
#./libwebp/enc/quant.c \
#./libwebp/enc/syntax.c \
#./libwebp/enc/token.c \
#./libwebp/enc/tree.c \
#./libwebp/enc/vp8l.c \
#./libwebp/enc/webpenc.c \
#./libwebp/utils/bit_reader.c \
#./libwebp/utils/bit_writer.c \
#./libwebp/utils/color_cache.c \
#./libwebp/utils/filters.c \
#./libwebp/utils/huffman.c \
#./libwebp/utils/huffman_encode.c \
#./libwebp/utils/quant_levels.c \
#./libwebp/utils/quant_levels_dec.c \
#./libwebp/utils/random.c \
#./libwebp/utils/rescaler.c \
#./libwebp/utils/thread.c \
#./libwebp/utils/utils.c \
#
#include $(BUILD_STATIC_LIBRARY)


################################################################################
# libiconv
################################################################################


# rough howto
# - run the normal ./configure to create iconv.h
# - copy the needed files
# - in localchatset.c, avoid including langinfo.h

include $(CLEAR_VARS)
LOCAL_MODULE    := libiconv
LOCAL_CFLAGS    := \
    -Wno-multichar \
    -D_ANDROID \
    -DLIBDIR="\"c\"" \
    -DBUILDING_LIBICONV \
    -DIN_LIBRARY
LOCAL_C_INCLUDES := \
	$(LOCAL_PATH)/libiconv/ \
    $(LOCAL_PATH)/libiconv/include \
    $(LOCAL_PATH)/libiconv/lib \
    $(LOCAL_PATH)/libiconv/libcharset/include
LOCAL_SRC_FILES := \
    ./libiconv/lib/iconv.c \
    ./libiconv/lib/relocatable.c  \
    ./libiconv/libcharset/lib/localcharset.c

include $(BUILD_STATIC_LIBRARY)


################################################################################
# libetpan
################################################################################


# rough howto:
# - copy files from original source
# - create include file links using "./autogen.sh; cd include; make" (or so)
# - use config.h from libetpan/build-android/include
# - use libetpan-config.h from local installation 


include $(CLEAR_VARS)

LOCAL_MODULE := etpan
LOCAL_CFLAGS += -DHAVE_CONFIG_H=1 -DHAVE_ICONV=1
LOCAL_SRC_FILES := \
	./messenger-backend/libs/libetpan/src/data-types/base64.c \
	./messenger-backend/libs/libetpan/src/data-types/carray.c \
	./messenger-backend/libs/libetpan/src/data-types/charconv.c \
	./messenger-backend/libs/libetpan/src/data-types/chash.c \
	./messenger-backend/libs/libetpan/src/data-types/clist.c \
	./messenger-backend/libs/libetpan/src/data-types/connect.c \
	./messenger-backend/libs/libetpan/src/data-types/mail_cache_db.c \
	./messenger-backend/libs/libetpan/src/data-types/maillock.c \
	./messenger-backend/libs/libetpan/src/data-types/mailsasl.c \
	./messenger-backend/libs/libetpan/src/data-types/mailsem.c \
	./messenger-backend/libs/libetpan/src/data-types/mailstream.c \
	./messenger-backend/libs/libetpan/src/data-types/mailstream_cancel.c \
	./messenger-backend/libs/libetpan/src/data-types/mailstream_cfstream.c \
	./messenger-backend/libs/libetpan/src/data-types/mailstream_compress.c \
	./messenger-backend/libs/libetpan/src/data-types/mailstream_helper.c \
	./messenger-backend/libs/libetpan/src/data-types/mailstream_low.c \
	./messenger-backend/libs/libetpan/src/data-types/mailstream_socket.c \
	./messenger-backend/libs/libetpan/src/data-types/mailstream_ssl.c \
	./messenger-backend/libs/libetpan/src/data-types/md5.c \
	./messenger-backend/libs/libetpan/src/data-types/mmapstring.c \
	./messenger-backend/libs/libetpan/src/data-types/timeutils.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/acl.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/acl_parser.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/acl_sender.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/acl_types.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/annotatemore.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/annotatemore_parser.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/annotatemore_sender.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/annotatemore_types.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/condstore.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/condstore_types.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/enable.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/idle.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_compress.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_extension.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_helper.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_id.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_id_parser.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_id_sender.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_id_types.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_keywords.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_oauth2.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_parser.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_print.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_sender.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_socket.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_sort.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_sort_types.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_ssl.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_types.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/mailimap_types_helper.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/namespace.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/namespace_parser.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/namespace_sender.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/namespace_types.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/qresync.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/qresync_types.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/quota.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/quota_parser.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/quota_sender.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/quota_types.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/uidplus.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/uidplus_parser.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/uidplus_sender.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/uidplus_types.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/xgmlabels.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/xgmmsgid.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/xgmthrid.c \
	./messenger-backend/libs/libetpan/src/low-level/imap/xlist.c \
	./messenger-backend/libs/libetpan/src/low-level/imf/mailimf.c \
	./messenger-backend/libs/libetpan/src/low-level/imf/mailimf_types.c \
	./messenger-backend/libs/libetpan/src/low-level/imf/mailimf_types_helper.c \
	./messenger-backend/libs/libetpan/src/low-level/imf/mailimf_write_file.c \
	./messenger-backend/libs/libetpan/src/low-level/imf/mailimf_write_generic.c \
	./messenger-backend/libs/libetpan/src/low-level/imf/mailimf_write_mem.c \
	./messenger-backend/libs/libetpan/src/low-level/mime/mailmime.c \
	./messenger-backend/libs/libetpan/src/low-level/mime/mailmime_content.c \
	./messenger-backend/libs/libetpan/src/low-level/mime/mailmime_decode.c \
	./messenger-backend/libs/libetpan/src/low-level/mime/mailmime_disposition.c \
	./messenger-backend/libs/libetpan/src/low-level/mime/mailmime_types.c \
	./messenger-backend/libs/libetpan/src/low-level/mime/mailmime_types_helper.c \
	./messenger-backend/libs/libetpan/src/low-level/mime/mailmime_write_file.c \
	./messenger-backend/libs/libetpan/src/low-level/mime/mailmime_write_generic.c \
	./messenger-backend/libs/libetpan/src/low-level/mime/mailmime_write_mem.c \
	./messenger-backend/libs/libetpan/src/low-level/nntp/newsnntp.c \
	./messenger-backend/libs/libetpan/src/low-level/nntp/newsnntp_socket.c \
	./messenger-backend/libs/libetpan/src/low-level/nntp/newsnntp_ssl.c \
	./messenger-backend/libs/libetpan/src/low-level/smtp/mailsmtp.c \
	./messenger-backend/libs/libetpan/src/low-level/smtp/mailsmtp_helper.c \
	./messenger-backend/libs/libetpan/src/low-level/smtp/mailsmtp_oauth2.c \
	./messenger-backend/libs/libetpan/src/low-level/smtp/mailsmtp_socket.c \
	./messenger-backend/libs/libetpan/src/low-level/smtp/mailsmtp_ssl.c \
	./messenger-backend/libs/libetpan/src/main/libetpan_version.c \
	./messenger-backend/libs/libetpan/src/driver/implementation/data-message/data_message_driver.c \
	./messenger-backend/libs/libetpan/src/driver/implementation/mime-message/mime_message_driver.c \
	./messenger-backend/libs/libetpan/src/driver/interface/maildriver.c \
	./messenger-backend/libs/libetpan/src/driver/interface/maildriver_tools.c \
	./messenger-backend/libs/libetpan/src/driver/interface/maildriver_types.c \
	./messenger-backend/libs/libetpan/src/driver/interface/maildriver_types_helper.c \
	./messenger-backend/libs/libetpan/src/driver/interface/mailfolder.c \
	./messenger-backend/libs/libetpan/src/driver/interface/mailmessage.c \
	./messenger-backend/libs/libetpan/src/driver/interface/mailmessage_tools.c \
	./messenger-backend/libs/libetpan/src/driver/interface/mailmessage_types.c \
	./messenger-backend/libs/libetpan/src/driver/interface/mailstorage.c \
	./messenger-backend/libs/libetpan/src/driver/interface/mailstorage_tools.c \
	./messenger-backend/libs/libetpan/src/engine/mailprivacy.c \
	./messenger-backend/libs/libetpan/src/engine/mailprivacy_tools.c
LOCAL_C_INCLUDES = \
	$(LOCAL_PATH)/messenger-backend/libs/libetpan/src \
	$(LOCAL_PATH)/messenger-backend/libs/libetpan/src/data-types \
	$(LOCAL_PATH)/messenger-backend/libs/libetpan/src/low-level \
	$(LOCAL_PATH)/messenger-backend/libs/libetpan/src/low-level/imap \
	$(LOCAL_PATH)/messenger-backend/libs/libetpan/src/low-level/imf \
	$(LOCAL_PATH)/messenger-backend/libs/libetpan/src/low-level/mime \
	$(LOCAL_PATH)/messenger-backend/libs/libetpan/src/low-level/nntp \
	$(LOCAL_PATH)/messenger-backend/libs/libetpan/src/low-level/smtp \
	$(LOCAL_PATH)/messenger-backend/libs/libetpan/src/main \
	$(LOCAL_PATH)/messenger-backend/libs/libetpan/src/driver/implementation/data-message \
	$(LOCAL_PATH)/messenger-backend/libs/libetpan/src/driver/interface \
	$(LOCAL_PATH)/messenger-backend/libs/libetpan/include \
	$(LOCAL_PATH)/messenger-backend/libs/libetpan/include/libetpan \
	$(LOCAL_PATH)/openssl/include \
	$(LOCAL_PATH)/libiconv/include \
	$(LOCAL_PATH)/cyrussasl/include \
	$(LOCAL_PATH)/cyrussasl/include/sasl

include $(BUILD_STATIC_LIBRARY)


################################################################################
# cyrus sasl
################################################################################


# rough howto:
# - copy files from original source
# - use config.h from libetpan/build-android/dependencies/cyrus-sasl/build-android/include

include $(CLEAR_VARS)

LOCAL_MODULE := sasl2

LOCAL_C_INCLUDES += \
	$(LOCAL_PATH)/cyrussasl/include \
	$(LOCAL_PATH)/cyrussasl/include/sasl \
	$(LOCAL_PATH)/cyrussasl/plugins \
	$(LOCAL_PATH)/openssl/include
LOCAL_SRC_FILES := \
	./cyrussasl/lib/auxprop.c \
	./cyrussasl/lib/canonusr.c \
	./cyrussasl/lib/checkpw.c \
	./cyrussasl/lib/client.c \
	./cyrussasl/lib/common.c \
	./cyrussasl/lib/config.c \
	./cyrussasl/lib/dlopen.c \
	./cyrussasl/lib/external.c \
	./cyrussasl/lib/getsubopt.c \
	./cyrussasl/lib/md5.c \
	./cyrussasl/lib/saslutil.c \
	./cyrussasl/lib/server.c \
	./cyrussasl/lib/seterror.c \
	./cyrussasl/lib/snprintf.c \
	./cyrussasl/plugins/anonymous.c \
	./cyrussasl/plugins/anonymous_init.c \
	./cyrussasl/plugins/cram.c \
	./cyrussasl/plugins/crammd5_init.c \
	./cyrussasl/plugins/digestmd5.c \
	./cyrussasl/plugins/digestmd5_init.c \
	./cyrussasl/plugins/login.c \
	./cyrussasl/plugins/login_init.c \
	./cyrussasl/plugins/ntlm.c \
	./cyrussasl/plugins/ntlm_init.c \
	./cyrussasl/plugins/otp.c \
	./cyrussasl/plugins/otp_init.c \
	./cyrussasl/plugins/passdss.c \
	./cyrussasl/plugins/passdss_init.c \
	./cyrussasl/plugins/plain.c \
	./cyrussasl/plugins/plain_init.c \
	./cyrussasl/plugins/plugin_common.c \
	./cyrussasl/plugins/scram.c \
	./cyrussasl/plugins/scram_init.c \
	./cyrussasl/plugins/srp.c \
	./cyrussasl/plugins/srp_init.c

include $(BUILD_STATIC_LIBRARY)


################################################################################
# main shared library as used from Java (includes the static ones)
################################################################################


include $(CLEAR_VARS)
ifeq ($(TARGET_ARCH_ABI),armeabi)
	LOCAL_ARM_MODE  := thumb
else
	LOCAL_ARM_MODE  := arm
endif
LOCAL_MODULE := sqlite
LOCAL_CFLAGS 	:= -w -std=c11 -Os -DNULL=0 -DSOCKLEN_T=socklen_t -DLOCALE_NOT_USED -D_LARGEFILE_SOURCE=1 -D_FILE_OFFSET_BITS=64
LOCAL_CFLAGS 	+= -DANDROID_NDK -DDISABLE_IMPORTGL -fno-strict-aliasing -fprefetch-loop-arrays -DAVOID_TABLES -DANDROID_TILE_BASED_DECODE -DANDROID_ARMV6_IDCT -DHAVE_STRCHRNUL=0
LOCAL_CFLAGS 	+= -DSQLITE_OMIT_LOAD_EXTENSION

LOCAL_SRC_FILES     := \
./messenger-backend/libs/sqlite/sqlite3.c

include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_PRELINK_MODULE := false

LOCAL_MODULE 	:= messenger.1
LOCAL_CFLAGS 	:= -w -std=c11 -Os -DNULL=0 -DSOCKLEN_T=socklen_t -DLOCALE_NOT_USED -D_LARGEFILE_SOURCE=1 -D_FILE_OFFSET_BITS=64
LOCAL_CFLAGS 	+= -Drestrict='' -D__EMX__ -DOPUS_BUILD -DFIXED_POINT -DUSE_ALLOCA -DHAVE_LRINT -DHAVE_LRINTF -fno-math-errno
LOCAL_CFLAGS 	+= -DANDROID_NDK -DDISABLE_IMPORTGL -fno-strict-aliasing -fprefetch-loop-arrays -DAVOID_TABLES -DANDROID_TILE_BASED_DECODE -DANDROID_ARMV6_IDCT -ffast-math -D__STDC_CONSTANT_MACROS
LOCAL_CPPFLAGS 	:= -DBSD=1 -ffast-math -Os -funroll-loops -std=c++11
LOCAL_LDLIBS 	:= -ljnigraphics -llog -lz -latomic
LOCAL_STATIC_LIBRARIES :=  etpan sasl2 sqlite crypto avformat avcodec avutil libiconv
# if you get "undefined reference" errors, the reason for this may be the _order_! Eg. libiconv as the first library does not work!
# "breakpad" was placed after "crypto", NativeLoader.cpp after mrwrapper.c

LOCAL_SRC_FILES     := \
./opus/src/opus.c \
./opus/src/opus_decoder.c \
./opus/src/opus_encoder.c \
./opus/src/opus_multistream.c \
./opus/src/opus_multistream_encoder.c \
./opus/src/opus_multistream_decoder.c \
./opus/src/repacketizer.c \
./opus/src/analysis.c \
./opus/src/mlp.c \
./opus/src/mlp_data.c

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_ARM_MODE := arm
    LOCAL_CPPFLAGS += -DLIBYUV_NEON
    LOCAL_CFLAGS += -DLIBYUV_NEON
else
    ifeq ($(TARGET_ARCH_ABI),armeabi)
	LOCAL_ARM_MODE  := arm

    else
        ifeq ($(TARGET_ARCH_ABI),x86)
	    LOCAL_CPPFLAGS += -Dx86fix
	    LOCAL_CFLAGS += -Dx86fix
	    LOCAL_ARM_MODE  := arm
	    LOCAL_SRC_FILE += \
	    ./libyuv/source/row_x86.asm
        endif
    endif
endif

LOCAL_SRC_FILES     += \
./opus/silk/CNG.c \
./opus/silk/code_signs.c \
./opus/silk/init_decoder.c \
./opus/silk/decode_core.c \
./opus/silk/decode_frame.c \
./opus/silk/decode_parameters.c \
./opus/silk/decode_indices.c \
./opus/silk/decode_pulses.c \
./opus/silk/decoder_set_fs.c \
./opus/silk/dec_API.c \
./opus/silk/enc_API.c \
./opus/silk/encode_indices.c \
./opus/silk/encode_pulses.c \
./opus/silk/gain_quant.c \
./opus/silk/interpolate.c \
./opus/silk/LP_variable_cutoff.c \
./opus/silk/NLSF_decode.c \
./opus/silk/NSQ.c \
./opus/silk/NSQ_del_dec.c \
./opus/silk/PLC.c \
./opus/silk/shell_coder.c \
./opus/silk/tables_gain.c \
./opus/silk/tables_LTP.c \
./opus/silk/tables_NLSF_CB_NB_MB.c \
./opus/silk/tables_NLSF_CB_WB.c \
./opus/silk/tables_other.c \
./opus/silk/tables_pitch_lag.c \
./opus/silk/tables_pulses_per_block.c \
./opus/silk/VAD.c \
./opus/silk/control_audio_bandwidth.c \
./opus/silk/quant_LTP_gains.c \
./opus/silk/VQ_WMat_EC.c \
./opus/silk/HP_variable_cutoff.c \
./opus/silk/NLSF_encode.c \
./opus/silk/NLSF_VQ.c \
./opus/silk/NLSF_unpack.c \
./opus/silk/NLSF_del_dec_quant.c \
./opus/silk/process_NLSFs.c \
./opus/silk/stereo_LR_to_MS.c \
./opus/silk/stereo_MS_to_LR.c \
./opus/silk/check_control_input.c \
./opus/silk/control_SNR.c \
./opus/silk/init_encoder.c \
./opus/silk/control_codec.c \
./opus/silk/A2NLSF.c \
./opus/silk/ana_filt_bank_1.c \
./opus/silk/biquad_alt.c \
./opus/silk/bwexpander_32.c \
./opus/silk/bwexpander.c \
./opus/silk/debug.c \
./opus/silk/decode_pitch.c \
./opus/silk/inner_prod_aligned.c \
./opus/silk/lin2log.c \
./opus/silk/log2lin.c \
./opus/silk/LPC_analysis_filter.c \
./opus/silk/LPC_inv_pred_gain.c \
./opus/silk/table_LSF_cos.c \
./opus/silk/NLSF2A.c \
./opus/silk/NLSF_stabilize.c \
./opus/silk/NLSF_VQ_weights_laroia.c \
./opus/silk/pitch_est_tables.c \
./opus/silk/resampler.c \
./opus/silk/resampler_down2_3.c \
./opus/silk/resampler_down2.c \
./opus/silk/resampler_private_AR2.c \
./opus/silk/resampler_private_down_FIR.c \
./opus/silk/resampler_private_IIR_FIR.c \
./opus/silk/resampler_private_up2_HQ.c \
./opus/silk/resampler_rom.c \
./opus/silk/sigm_Q15.c \
./opus/silk/sort.c \
./opus/silk/sum_sqr_shift.c \
./opus/silk/stereo_decode_pred.c \
./opus/silk/stereo_encode_pred.c \
./opus/silk/stereo_find_predictor.c \
./opus/silk/stereo_quant_pred.c

LOCAL_SRC_FILES     += \
./opus/silk/fixed/LTP_analysis_filter_FIX.c \
./opus/silk/fixed/LTP_scale_ctrl_FIX.c \
./opus/silk/fixed/corrMatrix_FIX.c \
./opus/silk/fixed/encode_frame_FIX.c \
./opus/silk/fixed/find_LPC_FIX.c \
./opus/silk/fixed/find_LTP_FIX.c \
./opus/silk/fixed/find_pitch_lags_FIX.c \
./opus/silk/fixed/find_pred_coefs_FIX.c \
./opus/silk/fixed/noise_shape_analysis_FIX.c \
./opus/silk/fixed/prefilter_FIX.c \
./opus/silk/fixed/process_gains_FIX.c \
./opus/silk/fixed/regularize_correlations_FIX.c \
./opus/silk/fixed/residual_energy16_FIX.c \
./opus/silk/fixed/residual_energy_FIX.c \
./opus/silk/fixed/solve_LS_FIX.c \
./opus/silk/fixed/warped_autocorrelation_FIX.c \
./opus/silk/fixed/apply_sine_window_FIX.c \
./opus/silk/fixed/autocorr_FIX.c \
./opus/silk/fixed/burg_modified_FIX.c \
./opus/silk/fixed/k2a_FIX.c \
./opus/silk/fixed/k2a_Q16_FIX.c \
./opus/silk/fixed/pitch_analysis_core_FIX.c \
./opus/silk/fixed/vector_ops_FIX.c \
./opus/silk/fixed/schur64_FIX.c \
./opus/silk/fixed/schur_FIX.c

LOCAL_SRC_FILES     += \
./opus/celt/bands.c \
./opus/celt/celt.c \
./opus/celt/celt_encoder.c \
./opus/celt/celt_decoder.c \
./opus/celt/cwrs.c \
./opus/celt/entcode.c \
./opus/celt/entdec.c \
./opus/celt/entenc.c \
./opus/celt/kiss_fft.c \
./opus/celt/laplace.c \
./opus/celt/mathops.c \
./opus/celt/mdct.c \
./opus/celt/modes.c \
./opus/celt/pitch.c \
./opus/celt/celt_lpc.c \
./opus/celt/quant_bands.c \
./opus/celt/rate.c \
./opus/celt/vq.c \
./opus/celt/arm/armcpu.c \
./opus/celt/arm/arm_celt_map.c

LOCAL_SRC_FILES     += \
./opus/ogg/bitwise.c \
./opus/ogg/framing.c \
./opus/opusfile/info.c \
./opus/opusfile/internal.c \
./opus/opusfile/opusfile.c \
./opus/opusfile/stream.c

LOCAL_C_INCLUDES    := \
$(LOCAL_PATH)/opus/include \
$(LOCAL_PATH)/opus/silk \
$(LOCAL_PATH)/opus/silk/fixed \
$(LOCAL_PATH)/opus/celt \
$(LOCAL_PATH)/opus/ \
$(LOCAL_PATH)/opus/opusfile \
$(LOCAL_PATH)/libyuv/include \
$(LOCAL_PATH)/openssl/include \
$(LOCAL_PATH)/breakpad/common/android/include \
$(LOCAL_PATH)/breakpad \
$(LOCAL_PATH)/ffmpeg \
$(LOCAL_PATH)/messenger-backend/libs/libetpan/include \
$(LOCAL_PATH)/messenger-backend/libs/netpgp/include \
$(LOCAL_PATH)/messenger-backend/libs/sqlite

#LOCAL_SRC_FILES     += \
#./libjpeg/jcapimin.c \
#./libjpeg/jcapistd.c \
#./libjpeg/armv6_idct.S \
#./libjpeg/jccoefct.c \
#./libjpeg/jccolor.c \
#./libjpeg/jcdctmgr.c \
#./libjpeg/jchuff.c \
#./libjpeg/jcinit.c \
#./libjpeg/jcmainct.c \
#./libjpeg/jcmarker.c \
#./libjpeg/jcmaster.c \
#./libjpeg/jcomapi.c \
#./libjpeg/jcparam.c \
#./libjpeg/jcphuff.c \
#./libjpeg/jcprepct.c \
#./libjpeg/jcsample.c \
#./libjpeg/jctrans.c \
#./libjpeg/jdapimin.c \
#./libjpeg/jdapistd.c \
#./libjpeg/jdatadst.c \
#./libjpeg/jdatasrc.c \
#./libjpeg/jdcoefct.c \
#./libjpeg/jdcolor.c \
#./libjpeg/jddctmgr.c \
#./libjpeg/jdhuff.c \
#./libjpeg/jdinput.c \
#./libjpeg/jdmainct.c \
#./libjpeg/jdmarker.c \
#./libjpeg/jdmaster.c \
#./libjpeg/jdmerge.c \
#./libjpeg/jdphuff.c \
#./libjpeg/jdpostct.c \
#./libjpeg/jdsample.c \
#./libjpeg/jdtrans.c \
#./libjpeg/jerror.c \
#./libjpeg/jfdctflt.c \
#./libjpeg/jfdctfst.c \
#./libjpeg/jfdctint.c \
#./libjpeg/jidctflt.c \
#./libjpeg/jidctfst.c \
#./libjpeg/jidctint.c \
#./libjpeg/jidctred.c \
#./libjpeg/jmemmgr.c \
#./libjpeg/jmemnobs.c \
#./libjpeg/jquant1.c \
#./libjpeg/jquant2.c \
#./libjpeg/jutils.c

LOCAL_SRC_FILES     += \
./libyuv/source/compare_common.cc \
./libyuv/source/compare_gcc.cc \
./libyuv/source/compare_neon64.cc \
./libyuv/source/compare_win.cc \
./libyuv/source/compare.cc \
./libyuv/source/convert_argb.cc \
./libyuv/source/convert_from_argb.cc \
./libyuv/source/convert_from.cc \
./libyuv/source/convert_jpeg.cc \
./libyuv/source/convert_to_argb.cc \
./libyuv/source/convert_to_i420.cc \
./libyuv/source/convert.cc \
./libyuv/source/cpu_id.cc \
./libyuv/source/mjpeg_decoder.cc \
./libyuv/source/mjpeg_validate.cc \
./libyuv/source/planar_functions.cc \
./libyuv/source/rotate_any.cc \
./libyuv/source/rotate_argb.cc \
./libyuv/source/rotate_common.cc \
./libyuv/source/rotate_gcc.cc \
./libyuv/source/rotate_mips.cc \
./libyuv/source/rotate_neon64.cc \
./libyuv/source/rotate_win.cc \
./libyuv/source/rotate.cc \
./libyuv/source/row_any.cc \
./libyuv/source/row_common.cc \
./libyuv/source/row_gcc.cc \
./libyuv/source/row_mips.cc \
./libyuv/source/row_neon64.cc \
./libyuv/source/row_win.cc \
./libyuv/source/scale_any.cc \
./libyuv/source/scale_argb.cc \
./libyuv/source/scale_common.cc \
./libyuv/source/scale_gcc.cc \
./libyuv/source/scale_mips.cc \
./libyuv/source/scale_neon64.cc \
./libyuv/source/scale_win.cc \
./libyuv/source/scale.cc \
./libyuv/source/video_common.cc

ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
    LOCAL_CFLAGS += -DLIBYUV_NEON
    LOCAL_SRC_FILES += \
        ./libyuv/source/compare_neon.cc.neon    \
        ./libyuv/source/rotate_neon.cc.neon     \
        ./libyuv/source/row_neon.cc.neon        \
        ./libyuv/source/scale_neon.cc.neon
endif

LOCAL_SRC_FILES     += \
./mrjnimain.c \
./audio.c \
./image.c \
./video.c \
./gifvideo.cpp \
./messenger-backend/libs/netpgp/src/compress.c \
./messenger-backend/libs/netpgp/src/create.c \
./messenger-backend/libs/netpgp/src/crypto.c \
./messenger-backend/libs/netpgp/src/keyring.c \
./messenger-backend/libs/netpgp/src/misc.c \
./messenger-backend/libs/netpgp/src/openssl_crypto.c \
./messenger-backend/libs/netpgp/src/packet-parse.c \
./messenger-backend/libs/netpgp/src/packet-show.c \
./messenger-backend/libs/netpgp/src/reader.c \
./messenger-backend/libs/netpgp/src/signature.c \
./messenger-backend/libs/netpgp/src/symmetric.c \
./messenger-backend/libs/netpgp/src/validate.c \
./messenger-backend/libs/netpgp/src/writer.c \
./messenger-backend/src/mraheader.c \
./messenger-backend/src/mrapeerstate.c \
./messenger-backend/src/mrchat.c \
./messenger-backend/src/mrchatlist.c \
./messenger-backend/src/mrcmdline.c \
./messenger-backend/src/mrcontact.c \
./messenger-backend/src/mre2ee.c \
./messenger-backend/src/mre2ee_driver_openssl.c \
./messenger-backend/src/mrimap.c \
./messenger-backend/src/mrjob.c \
./messenger-backend/src/mrkey.c \
./messenger-backend/src/mrkeyring.c \
./messenger-backend/src/mrloginparam.c \
./messenger-backend/src/mrmailbox.c \
./messenger-backend/src/mrmailbox_configure.c \
./messenger-backend/src/mrmailbox_log.c \
./messenger-backend/src/mrmimeparser.c \
./messenger-backend/src/mrmsg.c \
./messenger-backend/src/mrosnative.c \
./messenger-backend/src/mrparam.c \
./messenger-backend/src/mrpoortext.c \
./messenger-backend/src/mrsaxparser.c \
./messenger-backend/src/mrsimplify.c \
./messenger-backend/src/mrsmtp.c \
./messenger-backend/src/mrsqlite3.c \
./messenger-backend/src/mrstock.c \
./messenger-backend/src/mrtools.c \
./mrwrapper.c

include $(BUILD_SHARED_LIBRARY)

$(call import-module,android/cpufeatures)
