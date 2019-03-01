JNI_DIR := $(call my-dir)
LOCAL_PATH := $(call my-dir)

################################################################################
# openssl - libcrypto
################################################################################

include $(CLEAR_VARS)


arm_cflags := -DOPENSSL_BN_ASM_MONT -DAES_ASM -DSHA1_ASM -DSHA256_ASM -DSHA512_ASM

local_src_files := \
	./messenger-backend/libs/openssl/crypto/cryptlib.c \
	./messenger-backend/libs/openssl/crypto/mem.c \
	./messenger-backend/libs/openssl/crypto/mem_clr.c \
	./messenger-backend/libs/openssl/crypto/mem_dbg.c \
	./messenger-backend/libs/openssl/crypto/cversion.c \
	./messenger-backend/libs/openssl/crypto/ex_data.c \
	./messenger-backend/libs/openssl/crypto/cpt_err.c \
	./messenger-backend/libs/openssl/crypto/ebcdic.c \
	./messenger-backend/libs/openssl/crypto/uid.c \
	./messenger-backend/libs/openssl/crypto/o_time.c \
	./messenger-backend/libs/openssl/crypto/o_str.c \
	./messenger-backend/libs/openssl/crypto/o_dir.c \
	./messenger-backend/libs/openssl/crypto/o_init.c \
	./messenger-backend/libs/openssl/crypto/aes/aes_cbc.c \
	./messenger-backend/libs/openssl/crypto/aes/aes_core.c \
	./messenger-backend/libs/openssl/crypto/aes/aes_cfb.c \
	./messenger-backend/libs/openssl/crypto/aes/aes_ctr.c \
	./messenger-backend/libs/openssl/crypto/aes/aes_ecb.c \
	./messenger-backend/libs/openssl/crypto/aes/aes_ige.c \
	./messenger-backend/libs/openssl/crypto/aes/aes_misc.c \
	./messenger-backend/libs/openssl/crypto/aes/aes_ofb.c \
	./messenger-backend/libs/openssl/crypto/aes/aes_wrap.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_bitstr.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_bool.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_bytes.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_d2i_fp.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_digest.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_dup.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_enum.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_gentm.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_i2d_fp.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_int.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_mbstr.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_object.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_octet.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_print.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_set.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_sign.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_strex.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_strnid.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_time.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_type.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_utctm.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_utf8.c \
	./messenger-backend/libs/openssl/crypto/asn1/a_verify.c \
	./messenger-backend/libs/openssl/crypto/asn1/ameth_lib.c \
	./messenger-backend/libs/openssl/crypto/asn1/asn1_err.c \
	./messenger-backend/libs/openssl/crypto/asn1/asn1_gen.c \
	./messenger-backend/libs/openssl/crypto/asn1/asn1_lib.c \
	./messenger-backend/libs/openssl/crypto/asn1/asn1_par.c \
	./messenger-backend/libs/openssl/crypto/asn1/asn_mime.c \
	./messenger-backend/libs/openssl/crypto/asn1/asn_moid.c \
	./messenger-backend/libs/openssl/crypto/asn1/asn_pack.c \
	./messenger-backend/libs/openssl/crypto/asn1/bio_asn1.c \
	./messenger-backend/libs/openssl/crypto/asn1/bio_ndef.c \
	./messenger-backend/libs/openssl/crypto/asn1/d2i_pr.c \
	./messenger-backend/libs/openssl/crypto/asn1/d2i_pu.c \
	./messenger-backend/libs/openssl/crypto/asn1/evp_asn1.c \
	./messenger-backend/libs/openssl/crypto/asn1/f_enum.c \
	./messenger-backend/libs/openssl/crypto/asn1/f_int.c \
	./messenger-backend/libs/openssl/crypto/asn1/f_string.c \
	./messenger-backend/libs/openssl/crypto/asn1/i2d_pr.c \
	./messenger-backend/libs/openssl/crypto/asn1/i2d_pu.c \
	./messenger-backend/libs/openssl/crypto/asn1/n_pkey.c \
	./messenger-backend/libs/openssl/crypto/asn1/nsseq.c \
	./messenger-backend/libs/openssl/crypto/asn1/p5_pbe.c \
	./messenger-backend/libs/openssl/crypto/asn1/p5_pbev2.c \
	./messenger-backend/libs/openssl/crypto/asn1/p8_pkey.c \
	./messenger-backend/libs/openssl/crypto/asn1/t_bitst.c \
	./messenger-backend/libs/openssl/crypto/asn1/t_crl.c \
	./messenger-backend/libs/openssl/crypto/asn1/t_pkey.c \
	./messenger-backend/libs/openssl/crypto/asn1/t_req.c \
	./messenger-backend/libs/openssl/crypto/asn1/t_spki.c \
	./messenger-backend/libs/openssl/crypto/asn1/t_x509.c \
	./messenger-backend/libs/openssl/crypto/asn1/t_x509a.c \
	./messenger-backend/libs/openssl/crypto/asn1/tasn_dec.c \
	./messenger-backend/libs/openssl/crypto/asn1/tasn_enc.c \
	./messenger-backend/libs/openssl/crypto/asn1/tasn_fre.c \
	./messenger-backend/libs/openssl/crypto/asn1/tasn_new.c \
	./messenger-backend/libs/openssl/crypto/asn1/tasn_prn.c \
	./messenger-backend/libs/openssl/crypto/asn1/tasn_typ.c \
	./messenger-backend/libs/openssl/crypto/asn1/tasn_utl.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_algor.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_attrib.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_bignum.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_crl.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_exten.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_info.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_long.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_name.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_nx509.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_pkey.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_pubkey.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_req.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_sig.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_spki.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_val.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_x509.c \
	./messenger-backend/libs/openssl/crypto/asn1/x_x509a.c \
	./messenger-backend/libs/openssl/crypto/bf/bf_cfb64.c \
	./messenger-backend/libs/openssl/crypto/bf/bf_ecb.c \
	./messenger-backend/libs/openssl/crypto/bf/bf_enc.c \
	./messenger-backend/libs/openssl/crypto/bf/bf_ofb64.c \
	./messenger-backend/libs/openssl/crypto/bf/bf_skey.c \
	./messenger-backend/libs/openssl/crypto/bio/b_dump.c \
	./messenger-backend/libs/openssl/crypto/bio/b_print.c \
	./messenger-backend/libs/openssl/crypto/bio/b_sock.c \
	./messenger-backend/libs/openssl/crypto/bio/bf_buff.c \
	./messenger-backend/libs/openssl/crypto/bio/bf_nbio.c \
	./messenger-backend/libs/openssl/crypto/bio/bf_null.c \
	./messenger-backend/libs/openssl/crypto/bio/bio_cb.c \
	./messenger-backend/libs/openssl/crypto/bio/bio_err.c \
	./messenger-backend/libs/openssl/crypto/bio/bio_lib.c \
	./messenger-backend/libs/openssl/crypto/bio/bss_acpt.c \
	./messenger-backend/libs/openssl/crypto/bio/bss_bio.c \
	./messenger-backend/libs/openssl/crypto/bio/bss_conn.c \
	./messenger-backend/libs/openssl/crypto/bio/bss_dgram.c \
	./messenger-backend/libs/openssl/crypto/bio/bss_fd.c \
	./messenger-backend/libs/openssl/crypto/bio/bss_file.c \
	./messenger-backend/libs/openssl/crypto/bio/bss_log.c \
	./messenger-backend/libs/openssl/crypto/bio/bss_mem.c \
	./messenger-backend/libs/openssl/crypto/bio/bss_null.c \
	./messenger-backend/libs/openssl/crypto/bio/bss_sock.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_add.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_asm.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_blind.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_ctx.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_div.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_err.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_exp.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_exp2.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_gcd.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_gf2m.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_kron.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_lib.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_mod.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_mont.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_mpi.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_mul.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_nist.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_prime.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_print.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_rand.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_recp.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_shift.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_sqr.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_sqrt.c \
	./messenger-backend/libs/openssl/crypto/bn/bn_word.c \
	./messenger-backend/libs/openssl/crypto/buffer/buf_err.c \
	./messenger-backend/libs/openssl/crypto/buffer/buf_str.c \
	./messenger-backend/libs/openssl/crypto/buffer/buffer.c \
	./messenger-backend/libs/openssl/crypto/camellia/camellia.c \
	./messenger-backend/libs/openssl/crypto/camellia/cmll_cbc.c \
	./messenger-backend/libs/openssl/crypto/camellia/cmll_cfb.c \
	./messenger-backend/libs/openssl/crypto/camellia/cmll_ctr.c \
	./messenger-backend/libs/openssl/crypto/camellia/cmll_ecb.c \
	./messenger-backend/libs/openssl/crypto/camellia/cmll_misc.c \
	./messenger-backend/libs/openssl/crypto/camellia/cmll_ofb.c \
	./messenger-backend/libs/openssl/crypto/camellia/cmll_utl.c \
	./messenger-backend/libs/openssl/crypto/cast/c_cfb64.c \
	./messenger-backend/libs/openssl/crypto/cast/c_ecb.c \
	./messenger-backend/libs/openssl/crypto/cast/c_enc.c \
	./messenger-backend/libs/openssl/crypto/cast/c_ofb64.c \
	./messenger-backend/libs/openssl/crypto/cast/c_skey.c \
	./messenger-backend/libs/openssl/crypto/cmac/cm_ameth.c \
	./messenger-backend/libs/openssl/crypto/cmac/cm_pmeth.c \
	./messenger-backend/libs/openssl/crypto/cmac/cmac.c \
	./messenger-backend/libs/openssl/crypto/comp/c_rle.c \
	./messenger-backend/libs/openssl/crypto/comp/c_zlib.c \
	./messenger-backend/libs/openssl/crypto/comp/comp_err.c \
	./messenger-backend/libs/openssl/crypto/comp/comp_lib.c \
	./messenger-backend/libs/openssl/crypto/conf/conf_api.c \
	./messenger-backend/libs/openssl/crypto/conf/conf_def.c \
	./messenger-backend/libs/openssl/crypto/conf/conf_err.c \
	./messenger-backend/libs/openssl/crypto/conf/conf_lib.c \
	./messenger-backend/libs/openssl/crypto/conf/conf_mall.c \
	./messenger-backend/libs/openssl/crypto/conf/conf_mod.c \
	./messenger-backend/libs/openssl/crypto/conf/conf_sap.c \
	./messenger-backend/libs/openssl/crypto/des/cbc_cksm.c \
	./messenger-backend/libs/openssl/crypto/des/cbc_enc.c \
	./messenger-backend/libs/openssl/crypto/des/cfb64ede.c \
	./messenger-backend/libs/openssl/crypto/des/cfb64enc.c \
	./messenger-backend/libs/openssl/crypto/des/cfb_enc.c \
	./messenger-backend/libs/openssl/crypto/des/des_enc.c \
	./messenger-backend/libs/openssl/crypto/des/des_old.c \
	./messenger-backend/libs/openssl/crypto/des/des_old2.c \
	./messenger-backend/libs/openssl/crypto/des/ecb3_enc.c \
	./messenger-backend/libs/openssl/crypto/des/ecb_enc.c \
	./messenger-backend/libs/openssl/crypto/des/ede_cbcm_enc.c \
	./messenger-backend/libs/openssl/crypto/des/enc_read.c \
	./messenger-backend/libs/openssl/crypto/des/enc_writ.c \
	./messenger-backend/libs/openssl/crypto/des/fcrypt.c \
	./messenger-backend/libs/openssl/crypto/des/fcrypt_b.c \
	./messenger-backend/libs/openssl/crypto/des/ofb64ede.c \
	./messenger-backend/libs/openssl/crypto/des/ofb64enc.c \
	./messenger-backend/libs/openssl/crypto/des/ofb_enc.c \
	./messenger-backend/libs/openssl/crypto/des/pcbc_enc.c \
	./messenger-backend/libs/openssl/crypto/des/qud_cksm.c \
	./messenger-backend/libs/openssl/crypto/des/rand_key.c \
	./messenger-backend/libs/openssl/crypto/des/read2pwd.c \
	./messenger-backend/libs/openssl/crypto/des/rpc_enc.c \
	./messenger-backend/libs/openssl/crypto/des/set_key.c \
	./messenger-backend/libs/openssl/crypto/des/str2key.c \
	./messenger-backend/libs/openssl/crypto/des/xcbc_enc.c \
	./messenger-backend/libs/openssl/crypto/dh/dh_ameth.c \
	./messenger-backend/libs/openssl/crypto/dh/dh_asn1.c \
	./messenger-backend/libs/openssl/crypto/dh/dh_check.c \
	./messenger-backend/libs/openssl/crypto/dh/dh_depr.c \
	./messenger-backend/libs/openssl/crypto/dh/dh_err.c \
	./messenger-backend/libs/openssl/crypto/dh/dh_gen.c \
	./messenger-backend/libs/openssl/crypto/dh/dh_key.c \
	./messenger-backend/libs/openssl/crypto/dh/dh_lib.c \
	./messenger-backend/libs/openssl/crypto/dh/dh_pmeth.c \
	./messenger-backend/libs/openssl/crypto/dsa/dsa_ameth.c \
	./messenger-backend/libs/openssl/crypto/dsa/dsa_asn1.c \
	./messenger-backend/libs/openssl/crypto/dsa/dsa_depr.c \
	./messenger-backend/libs/openssl/crypto/dsa/dsa_err.c \
	./messenger-backend/libs/openssl/crypto/dsa/dsa_gen.c \
	./messenger-backend/libs/openssl/crypto/dsa/dsa_key.c \
	./messenger-backend/libs/openssl/crypto/dsa/dsa_lib.c \
	./messenger-backend/libs/openssl/crypto/dsa/dsa_ossl.c \
	./messenger-backend/libs/openssl/crypto/dsa/dsa_pmeth.c \
	./messenger-backend/libs/openssl/crypto/dsa/dsa_prn.c \
	./messenger-backend/libs/openssl/crypto/dsa/dsa_sign.c \
	./messenger-backend/libs/openssl/crypto/dsa/dsa_vrf.c \
	./messenger-backend/libs/openssl/crypto/dso/dso_dl.c \
	./messenger-backend/libs/openssl/crypto/dso/dso_dlfcn.c \
	./messenger-backend/libs/openssl/crypto/dso/dso_err.c \
	./messenger-backend/libs/openssl/crypto/dso/dso_lib.c \
	./messenger-backend/libs/openssl/crypto/dso/dso_null.c \
	./messenger-backend/libs/openssl/crypto/dso/dso_openssl.c \
	./messenger-backend/libs/openssl/crypto/dso/dso_vms.c \
	./messenger-backend/libs/openssl/crypto/dso/dso_win32.c \
	./messenger-backend/libs/openssl/crypto/ec/ec2_mult.c \
	./messenger-backend/libs/openssl/crypto/ec/ec2_oct.c \
	./messenger-backend/libs/openssl/crypto/ec/ec2_smpl.c \
	./messenger-backend/libs/openssl/crypto/ec/ec_ameth.c \
	./messenger-backend/libs/openssl/crypto/ec/ec_asn1.c \
	./messenger-backend/libs/openssl/crypto/ec/ec_check.c \
	./messenger-backend/libs/openssl/crypto/ec/ec_curve.c \
	./messenger-backend/libs/openssl/crypto/ec/ec_cvt.c \
	./messenger-backend/libs/openssl/crypto/ec/ec_err.c \
	./messenger-backend/libs/openssl/crypto/ec/ec_key.c \
	./messenger-backend/libs/openssl/crypto/ec/ec_lib.c \
	./messenger-backend/libs/openssl/crypto/ec/ec_mult.c \
	./messenger-backend/libs/openssl/crypto/ec/ec_oct.c \
	./messenger-backend/libs/openssl/crypto/ec/ec_pmeth.c \
	./messenger-backend/libs/openssl/crypto/ec/ec_print.c \
	./messenger-backend/libs/openssl/crypto/ec/eck_prn.c \
	./messenger-backend/libs/openssl/crypto/ec/ecp_mont.c \
	./messenger-backend/libs/openssl/crypto/ec/ecp_nist.c \
	./messenger-backend/libs/openssl/crypto/ec/ecp_nistp224.c \
	./messenger-backend/libs/openssl/crypto/ec/ecp_nistp256.c \
	./messenger-backend/libs/openssl/crypto/ec/ecp_nistp521.c \
	./messenger-backend/libs/openssl/crypto/ec/ecp_nistputil.c \
	./messenger-backend/libs/openssl/crypto/ec/ecp_oct.c \
	./messenger-backend/libs/openssl/crypto/ec/ecp_smpl.c \
	./messenger-backend/libs/openssl/crypto/ecdh/ech_err.c \
	./messenger-backend/libs/openssl/crypto/ecdh/ech_key.c \
	./messenger-backend/libs/openssl/crypto/ecdh/ech_lib.c \
	./messenger-backend/libs/openssl/crypto/ecdh/ech_ossl.c \
	./messenger-backend/libs/openssl/crypto/ecdsa/ecs_asn1.c \
	./messenger-backend/libs/openssl/crypto/ecdsa/ecs_err.c \
	./messenger-backend/libs/openssl/crypto/ecdsa/ecs_lib.c \
	./messenger-backend/libs/openssl/crypto/ecdsa/ecs_ossl.c \
	./messenger-backend/libs/openssl/crypto/ecdsa/ecs_sign.c \
	./messenger-backend/libs/openssl/crypto/ecdsa/ecs_vrf.c \
	./messenger-backend/libs/openssl/crypto/err/err.c \
	./messenger-backend/libs/openssl/crypto/err/err_all.c \
	./messenger-backend/libs/openssl/crypto/err/err_prn.c \
	./messenger-backend/libs/openssl/crypto/evp/bio_b64.c \
	./messenger-backend/libs/openssl/crypto/evp/bio_enc.c \
	./messenger-backend/libs/openssl/crypto/evp/bio_md.c \
	./messenger-backend/libs/openssl/crypto/evp/bio_ok.c \
	./messenger-backend/libs/openssl/crypto/evp/c_all.c \
	./messenger-backend/libs/openssl/crypto/evp/c_allc.c \
	./messenger-backend/libs/openssl/crypto/evp/c_alld.c \
	./messenger-backend/libs/openssl/crypto/evp/digest.c \
	./messenger-backend/libs/openssl/crypto/evp/e_aes.c \
	./messenger-backend/libs/openssl/crypto/evp/e_aes_cbc_hmac_sha1.c \
	./messenger-backend/libs/openssl/crypto/evp/e_bf.c \
	./messenger-backend/libs/openssl/crypto/evp/e_camellia.c \
	./messenger-backend/libs/openssl/crypto/evp/e_cast.c \
	./messenger-backend/libs/openssl/crypto/evp/e_des.c \
	./messenger-backend/libs/openssl/crypto/evp/e_des3.c \
	./messenger-backend/libs/openssl/crypto/evp/e_null.c \
	./messenger-backend/libs/openssl/crypto/evp/e_old.c \
	./messenger-backend/libs/openssl/crypto/evp/e_rc2.c \
	./messenger-backend/libs/openssl/crypto/evp/e_rc4.c \
	./messenger-backend/libs/openssl/crypto/evp/e_rc4_hmac_md5.c \
	./messenger-backend/libs/openssl/crypto/evp/e_rc5.c \
	./messenger-backend/libs/openssl/crypto/evp/e_xcbc_d.c \
	./messenger-backend/libs/openssl/crypto/evp/encode.c \
	./messenger-backend/libs/openssl/crypto/evp/evp_acnf.c \
	./messenger-backend/libs/openssl/crypto/evp/evp_enc.c \
	./messenger-backend/libs/openssl/crypto/evp/evp_err.c \
	./messenger-backend/libs/openssl/crypto/evp/evp_key.c \
	./messenger-backend/libs/openssl/crypto/evp/evp_lib.c \
	./messenger-backend/libs/openssl/crypto/evp/evp_pbe.c \
	./messenger-backend/libs/openssl/crypto/evp/evp_pkey.c \
	./messenger-backend/libs/openssl/crypto/evp/m_dss.c \
	./messenger-backend/libs/openssl/crypto/evp/m_dss1.c \
	./messenger-backend/libs/openssl/crypto/evp/m_ecdsa.c \
	./messenger-backend/libs/openssl/crypto/evp/m_md4.c \
	./messenger-backend/libs/openssl/crypto/evp/m_md5.c \
	./messenger-backend/libs/openssl/crypto/evp/m_mdc2.c \
	./messenger-backend/libs/openssl/crypto/evp/m_null.c \
	./messenger-backend/libs/openssl/crypto/evp/m_ripemd.c \
	./messenger-backend/libs/openssl/crypto/evp/m_sha1.c \
	./messenger-backend/libs/openssl/crypto/evp/m_sigver.c \
	./messenger-backend/libs/openssl/crypto/evp/m_wp.c \
	./messenger-backend/libs/openssl/crypto/evp/names.c \
	./messenger-backend/libs/openssl/crypto/evp/p5_crpt.c \
	./messenger-backend/libs/openssl/crypto/evp/p5_crpt2.c \
	./messenger-backend/libs/openssl/crypto/evp/p_dec.c \
	./messenger-backend/libs/openssl/crypto/evp/p_enc.c \
	./messenger-backend/libs/openssl/crypto/evp/p_lib.c \
	./messenger-backend/libs/openssl/crypto/evp/p_open.c \
	./messenger-backend/libs/openssl/crypto/evp/p_seal.c \
	./messenger-backend/libs/openssl/crypto/evp/p_sign.c \
	./messenger-backend/libs/openssl/crypto/evp/p_verify.c \
	./messenger-backend/libs/openssl/crypto/evp/pmeth_fn.c \
	./messenger-backend/libs/openssl/crypto/evp/pmeth_gn.c \
	./messenger-backend/libs/openssl/crypto/evp/pmeth_lib.c \
	./messenger-backend/libs/openssl/crypto/hmac/hm_ameth.c \
	./messenger-backend/libs/openssl/crypto/hmac/hm_pmeth.c \
	./messenger-backend/libs/openssl/crypto/hmac/hmac.c \
	./messenger-backend/libs/openssl/crypto/krb5/krb5_asn.c \
	./messenger-backend/libs/openssl/crypto/lhash/lh_stats.c \
	./messenger-backend/libs/openssl/crypto/lhash/lhash.c \
	./messenger-backend/libs/openssl/crypto/md4/md4_dgst.c \
	./messenger-backend/libs/openssl/crypto/md4/md4_one.c \
	./messenger-backend/libs/openssl/crypto/md5/md5_dgst.c \
	./messenger-backend/libs/openssl/crypto/md5/md5_one.c \
	./messenger-backend/libs/openssl/crypto/modes/cbc128.c \
	./messenger-backend/libs/openssl/crypto/modes/ccm128.c \
	./messenger-backend/libs/openssl/crypto/modes/cfb128.c \
	./messenger-backend/libs/openssl/crypto/modes/ctr128.c \
	./messenger-backend/libs/openssl/crypto/modes/cts128.c \
	./messenger-backend/libs/openssl/crypto/modes/gcm128.c \
	./messenger-backend/libs/openssl/crypto/modes/ofb128.c \
	./messenger-backend/libs/openssl/crypto/modes/xts128.c \
	./messenger-backend/libs/openssl/crypto/objects/o_names.c \
	./messenger-backend/libs/openssl/crypto/objects/obj_dat.c \
	./messenger-backend/libs/openssl/crypto/objects/obj_err.c \
	./messenger-backend/libs/openssl/crypto/objects/obj_lib.c \
	./messenger-backend/libs/openssl/crypto/objects/obj_xref.c \
	./messenger-backend/libs/openssl/crypto/ocsp/ocsp_asn.c \
	./messenger-backend/libs/openssl/crypto/ocsp/ocsp_cl.c \
	./messenger-backend/libs/openssl/crypto/ocsp/ocsp_err.c \
	./messenger-backend/libs/openssl/crypto/ocsp/ocsp_ext.c \
	./messenger-backend/libs/openssl/crypto/ocsp/ocsp_ht.c \
	./messenger-backend/libs/openssl/crypto/ocsp/ocsp_lib.c \
	./messenger-backend/libs/openssl/crypto/ocsp/ocsp_prn.c \
	./messenger-backend/libs/openssl/crypto/ocsp/ocsp_srv.c \
	./messenger-backend/libs/openssl/crypto/ocsp/ocsp_vfy.c \
	./messenger-backend/libs/openssl/crypto/pem/pem_all.c \
	./messenger-backend/libs/openssl/crypto/pem/pem_err.c \
	./messenger-backend/libs/openssl/crypto/pem/pem_info.c \
	./messenger-backend/libs/openssl/crypto/pem/pem_lib.c \
	./messenger-backend/libs/openssl/crypto/pem/pem_oth.c \
	./messenger-backend/libs/openssl/crypto/pem/pem_pk8.c \
	./messenger-backend/libs/openssl/crypto/pem/pem_pkey.c \
	./messenger-backend/libs/openssl/crypto/pem/pem_seal.c \
	./messenger-backend/libs/openssl/crypto/pem/pem_sign.c \
	./messenger-backend/libs/openssl/crypto/pem/pem_x509.c \
	./messenger-backend/libs/openssl/crypto/pem/pem_xaux.c \
	./messenger-backend/libs/openssl/crypto/pem/pvkfmt.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_add.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_asn.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_attr.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_crpt.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_crt.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_decr.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_init.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_key.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_kiss.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_mutl.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_npas.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_p8d.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_p8e.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/p12_utl.c \
	./messenger-backend/libs/openssl/crypto/pkcs12/pk12err.c \
	./messenger-backend/libs/openssl/crypto/pkcs7/pk7_asn1.c \
	./messenger-backend/libs/openssl/crypto/pkcs7/pk7_attr.c \
	./messenger-backend/libs/openssl/crypto/pkcs7/pk7_doit.c \
	./messenger-backend/libs/openssl/crypto/pkcs7/pk7_lib.c \
	./messenger-backend/libs/openssl/crypto/pkcs7/pk7_mime.c \
	./messenger-backend/libs/openssl/crypto/pkcs7/pk7_smime.c \
	./messenger-backend/libs/openssl/crypto/pkcs7/pkcs7err.c \
	./messenger-backend/libs/openssl/crypto/pqueue/pqueue.c \
	./messenger-backend/libs/openssl/crypto/rand/md_rand.c \
	./messenger-backend/libs/openssl/crypto/rand/rand_egd.c \
	./messenger-backend/libs/openssl/crypto/rand/rand_err.c \
	./messenger-backend/libs/openssl/crypto/rand/rand_lib.c \
	./messenger-backend/libs/openssl/crypto/rand/rand_unix.c \
	./messenger-backend/libs/openssl/crypto/rand/randfile.c \
	./messenger-backend/libs/openssl/crypto/rc2/rc2_cbc.c \
	./messenger-backend/libs/openssl/crypto/rc2/rc2_ecb.c \
	./messenger-backend/libs/openssl/crypto/rc2/rc2_skey.c \
	./messenger-backend/libs/openssl/crypto/rc2/rc2cfb64.c \
	./messenger-backend/libs/openssl/crypto/rc2/rc2ofb64.c \
	./messenger-backend/libs/openssl/crypto/rc4/rc4_enc.c \
	./messenger-backend/libs/openssl/crypto/rc4/rc4_skey.c \
	./messenger-backend/libs/openssl/crypto/rc4/rc4_utl.c \
	./messenger-backend/libs/openssl/crypto/ripemd/rmd_dgst.c \
	./messenger-backend/libs/openssl/crypto/ripemd/rmd_one.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_ameth.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_asn1.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_chk.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_crpt.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_depr.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_eay.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_err.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_gen.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_lib.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_none.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_null.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_oaep.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_pk1.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_pmeth.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_prn.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_pss.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_saos.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_sign.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_ssl.c \
	./messenger-backend/libs/openssl/crypto/rsa/rsa_x931.c \
	./messenger-backend/libs/openssl/crypto/sha/sha1_one.c \
	./messenger-backend/libs/openssl/crypto/sha/sha1dgst.c \
	./messenger-backend/libs/openssl/crypto/sha/sha256.c \
	./messenger-backend/libs/openssl/crypto/sha/sha512.c \
	./messenger-backend/libs/openssl/crypto/sha/sha_dgst.c \
	./messenger-backend/libs/openssl/crypto/srp/srp_lib.c \
	./messenger-backend/libs/openssl/crypto/srp/srp_vfy.c \
	./messenger-backend/libs/openssl/crypto/stack/stack.c \
	./messenger-backend/libs/openssl/crypto/ts/ts_err.c \
	./messenger-backend/libs/openssl/crypto/txt_db/txt_db.c \
	./messenger-backend/libs/openssl/crypto/ui/ui_compat.c \
	./messenger-backend/libs/openssl/crypto/ui/ui_err.c \
	./messenger-backend/libs/openssl/crypto/ui/ui_lib.c \
	./messenger-backend/libs/openssl/crypto/ui/ui_openssl.c \
	./messenger-backend/libs/openssl/crypto/ui/ui_util.c \
	./messenger-backend/libs/openssl/crypto/x509/by_dir.c \
	./messenger-backend/libs/openssl/crypto/x509/by_file.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_att.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_cmp.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_d2.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_def.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_err.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_ext.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_lu.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_obj.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_r2x.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_req.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_set.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_trs.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_txt.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_v3.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_vfy.c \
	./messenger-backend/libs/openssl/crypto/x509/x509_vpm.c \
	./messenger-backend/libs/openssl/crypto/x509/x509cset.c \
	./messenger-backend/libs/openssl/crypto/x509/x509name.c \
	./messenger-backend/libs/openssl/crypto/x509/x509rset.c \
	./messenger-backend/libs/openssl/crypto/x509/x509spki.c \
	./messenger-backend/libs/openssl/crypto/x509/x509type.c \
	./messenger-backend/libs/openssl/crypto/x509/x_all.c \
	./messenger-backend/libs/openssl/crypto/x509v3/pcy_cache.c \
	./messenger-backend/libs/openssl/crypto/x509v3/pcy_data.c \
	./messenger-backend/libs/openssl/crypto/x509v3/pcy_lib.c \
	./messenger-backend/libs/openssl/crypto/x509v3/pcy_map.c \
	./messenger-backend/libs/openssl/crypto/x509v3/pcy_node.c \
	./messenger-backend/libs/openssl/crypto/x509v3/pcy_tree.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_akey.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_akeya.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_alt.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_bcons.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_bitst.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_conf.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_cpols.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_crld.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_enum.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_extku.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_genn.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_ia5.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_info.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_int.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_lib.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_ncons.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_ocsp.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_pci.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_pcia.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_pcons.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_pku.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_pmaps.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_prn.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_purp.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_skey.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_sxnet.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3_utl.c \
	./messenger-backend/libs/openssl/crypto/x509v3/v3err.c \
	./messenger-backend/libs/openssl/ssl/bio_ssl.c   \
	./messenger-backend/libs/openssl/ssl/s2_meth.c  \
	./messenger-backend/libs/openssl/ssl/ssl_algs.c  \
	./messenger-backend/libs/openssl/ssl/kssl.c      \
	./messenger-backend/libs/openssl/ssl/s2_pkt.c   \
	./messenger-backend/libs/openssl/ssl/ssl_asn1.c  \
	./messenger-backend/libs/openssl/ssl/d1_both.c   \
	./messenger-backend/libs/openssl/ssl/s2_srvr.c  \
	./messenger-backend/libs/openssl/ssl/ssl_cert.c  \
	./messenger-backend/libs/openssl/ssl/ssl_txt.c \
	./messenger-backend/libs/openssl/ssl/d1_clnt.c   \
	./messenger-backend/libs/openssl/ssl/s23_clnt.c  \
	./messenger-backend/libs/openssl/ssl/s3_both.c  \
	./messenger-backend/libs/openssl/ssl/ssl_ciph.c  \
	./messenger-backend/libs/openssl/ssl/ssl_utst.c \
	./messenger-backend/libs/openssl/ssl/d1_enc.c    \
	./messenger-backend/libs/openssl/ssl/s23_lib.c   \
	./messenger-backend/libs/openssl/ssl/s3_cbc.c   \
	./messenger-backend/libs/openssl/ssl/ssl_err2.c  \
	./messenger-backend/libs/openssl/ssl/t1_clnt.c \
	./messenger-backend/libs/openssl/ssl/d1_lib.c    \
	./messenger-backend/libs/openssl/ssl/s23_meth.c  \
	./messenger-backend/libs/openssl/ssl/s3_clnt.c  \
	./messenger-backend/libs/openssl/ssl/ssl_err.c   \
	./messenger-backend/libs/openssl/ssl/t1_enc.c \
	./messenger-backend/libs/openssl/ssl/d1_meth.c   \
	./messenger-backend/libs/openssl/ssl/s23_pkt.c   \
	./messenger-backend/libs/openssl/ssl/s3_enc.c   \
	./messenger-backend/libs/openssl/ssl/ssl_lib.c   \
	./messenger-backend/libs/openssl/ssl/t1_lib.c \
	./messenger-backend/libs/openssl/ssl/d1_pkt.c    \
	./messenger-backend/libs/openssl/ssl/s23_srvr.c  \
	./messenger-backend/libs/openssl/ssl/s3_lib.c   \
	./messenger-backend/libs/openssl/ssl/t1_meth.c \
	./messenger-backend/libs/openssl/ssl/d1_srtp.c   \
	./messenger-backend/libs/openssl/ssl/s2_clnt.c   \
	./messenger-backend/libs/openssl/ssl/s3_meth.c  \
	./messenger-backend/libs/openssl/ssl/ssl_rsa.c   \
	./messenger-backend/libs/openssl/ssl/t1_reneg.c \
	./messenger-backend/libs/openssl/ssl/d1_srvr.c   \
	./messenger-backend/libs/openssl/ssl/s2_enc.c    \
	./messenger-backend/libs/openssl/ssl/s3_pkt.c   \
	./messenger-backend/libs/openssl/ssl/ssl_sess.c  \
	./messenger-backend/libs/openssl/ssl/t1_srvr.c \
	./messenger-backend/libs/openssl/ssl/s2_lib.c    \
	./messenger-backend/libs/openssl/ssl/s3_srvr.c  \
	./messenger-backend/libs/openssl/ssl/ssl_stat.c  \
	./messenger-backend/libs/openssl/ssl/tls_srp.c

local_c_includes := \
	$(LOCAL_PATH)/messenger-backend/libs/openssl \
	$(LOCAL_PATH)/messenger-backend/libs/openssl/crypto \
	$(LOCAL_PATH)/messenger-backend/libs/openssl/crypto/asn1 \
	$(LOCAL_PATH)/messenger-backend/libs/openssl/crypto/evp \
	$(LOCAL_PATH)/messenger-backend/libs/openssl/crypto/modes \
	$(LOCAL_PATH)/messenger-backend/libs/openssl/include \
	$(LOCAL_PATH)/messenger-backend/libs/openssl/include/openssl

local_c_flags := -DNO_WINDOWS_BRAINDEATH

LOCAL_SRC_FILES += $(local_src_files)
LOCAL_CFLAGS += -DOPENSSL_THREADS -D_REENTRANT -DDSO_DLFCN -DHAVE_DLFCN_H -DL_ENDIAN
LOCAL_CFLAGS += -DOPENSSL_NO_CAPIENG -DOPENSSL_NO_CMS -DOPENSSL_NO_GMP -DOPENSSL_NO_IDEA -DOPENSSL_NO_JPAKE -DOPENSSL_NO_MD2 -DOPENSSL_NO_MDC2 -DOPENSSL_NO_RC5 -DOPENSSL_NO_SHA0 -DOPENSSL_NO_RFC3779 -DOPENSSL_NO_SEED -DOPENSSL_NO_STORE -DOPENSSL_NO_WHIRLPOOL
LOCAL_CFLAGS += -DOPENSSL_NO_HW -DOPENSSL_NO_ENGINE -DZLIB
LOCAL_CFLAGS += $(local_c_flags) -DPURIFY
LOCAL_C_INCLUDES += $(local_c_includes)
LOCAL_MODULE_TAGS := optional
LOCAL_MODULE:= libcrypto
include $(BUILD_STATIC_LIBRARY)

#include $(CLEAR_VARS)
#LOCAL_MODULE    := crypto
#
#ifeq ($(TARGET_ARCH_ABI),armeabi-v7a)
#    LOCAL_SRC_FILES := ./messenger-backend/libs/openssl/obj/local/armeabi-v7a/libcrypto.a
#else
#    ifeq ($(TARGET_ARCH_ABI),armeabi)
#       LOCAL_SRC_FILES := ./messenger-backend/libs/openssl/obj/local/armeabi/libcrypto.a
#    else
#        ifeq ($(TARGET_ARCH_ABI),x86)
#           LOCAL_SRC_FILES := ./messenger-backend/libs/openssl/obj/local/x86/libcrypto.a
#        endif
#    endif
#endif

#include $(PREBUILT_STATIC_LIBRARY)

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
    $(LOCAL_PATH)/messenger-backend/libs/libiconv/ \
    $(LOCAL_PATH)/messenger-backend/libs/libiconv/include \
    $(LOCAL_PATH)/messenger-backend/libs/libiconv/lib \
    $(LOCAL_PATH)/messenger-backend/libs/libiconv/libcharset/include
LOCAL_SRC_FILES := \
    ./messenger-backend/libs/libiconv/lib/iconv.c \
    ./messenger-backend/libs/libiconv/lib/relocatable.c  \
    ./messenger-backend/libs/libiconv/libcharset/lib/localcharset.c

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
	$(LOCAL_PATH)/messenger-backend/libs/openssl/include \
	$(LOCAL_PATH)/messenger-backend/libs/libiconv/include \
	$(LOCAL_PATH)/messenger-backend/libs/cyrussasl/include \
	$(LOCAL_PATH)/messenger-backend/libs/cyrussasl/include/sasl

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
	$(LOCAL_PATH)/messenger-backend/libs/cyrussasl/include \
	$(LOCAL_PATH)/messenger-backend/libs/cyrussasl/include/sasl \
	$(LOCAL_PATH)/messenger-backend/libs/cyrussasl/plugins \
	$(LOCAL_PATH)/messenger-backend/libs/openssl/include
LOCAL_SRC_FILES := \
	./messenger-backend/libs/cyrussasl/lib/auxprop.c \
	./messenger-backend/libs/cyrussasl/lib/canonusr.c \
	./messenger-backend/libs/cyrussasl/lib/checkpw.c \
	./messenger-backend/libs/cyrussasl/lib/client.c \
	./messenger-backend/libs/cyrussasl/lib/common.c \
	./messenger-backend/libs/cyrussasl/lib/config.c \
	./messenger-backend/libs/cyrussasl/lib/dlopen.c \
	./messenger-backend/libs/cyrussasl/lib/external.c \
	./messenger-backend/libs/cyrussasl/lib/getsubopt.c \
	./messenger-backend/libs/cyrussasl/lib/md5.c \
	./messenger-backend/libs/cyrussasl/lib/saslutil.c \
	./messenger-backend/libs/cyrussasl/lib/server.c \
	./messenger-backend/libs/cyrussasl/lib/seterror.c \
	./messenger-backend/libs/cyrussasl/lib/snprintf.c \
	./messenger-backend/libs/cyrussasl/plugins/anonymous.c \
	./messenger-backend/libs/cyrussasl/plugins/anonymous_init.c \
	./messenger-backend/libs/cyrussasl/plugins/cram.c \
	./messenger-backend/libs/cyrussasl/plugins/crammd5_init.c \
	./messenger-backend/libs/cyrussasl/plugins/digestmd5.c \
	./messenger-backend/libs/cyrussasl/plugins/digestmd5_init.c \
	./messenger-backend/libs/cyrussasl/plugins/login.c \
	./messenger-backend/libs/cyrussasl/plugins/login_init.c \
	./messenger-backend/libs/cyrussasl/plugins/ntlm.c \
	./messenger-backend/libs/cyrussasl/plugins/ntlm_init.c \
	./messenger-backend/libs/cyrussasl/plugins/otp.c \
	./messenger-backend/libs/cyrussasl/plugins/otp_init.c \
	./messenger-backend/libs/cyrussasl/plugins/passdss.c \
	./messenger-backend/libs/cyrussasl/plugins/passdss_init.c \
	./messenger-backend/libs/cyrussasl/plugins/plain.c \
	./messenger-backend/libs/cyrussasl/plugins/plain_init.c \
	./messenger-backend/libs/cyrussasl/plugins/plugin_common.c \
	./messenger-backend/libs/cyrussasl/plugins/scram.c \
	./messenger-backend/libs/cyrussasl/plugins/scram_init.c \
	./messenger-backend/libs/cyrussasl/plugins/srp.c \
	./messenger-backend/libs/cyrussasl/plugins/srp_init.c

include $(BUILD_STATIC_LIBRARY)

################################################################################
# sqlite
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
LOCAL_CFLAGS 	+= -DSQLITE_MAX_MMAP_SIZE=0 -DSQLITE_OMIT_WAL  # HACK: the defines are used to skip the pointer reference to mmap set in aSyscall[] - mmap seems to be a #define that cannot be used this way on Android - otherwise we get the error: 'mmap' undeclared here (not in a function)

LOCAL_SRC_FILES     := \
./messenger-backend/libs/sqlite/sqlite3.c

include $(BUILD_STATIC_LIBRARY)


################################################################################
# rpgp
################################################################################

# rpgp must be cloned to the same directory where deltachat-android is
# (not: inside deltachat-android)
#
# you can then cross-compile rpgp to android using
# > cross build --release --target armv7-linux-androideabi -p pgp-ffi --features nightly
#
# TODO: x86 and other processor targets are still missing, automation, -droid script

include $(CLEAR_VARS)

LOCAL_MODULE    := librpgp
LOCAL_SRC_FILES := ../../rpgp/target/armv7-linux-androideabi/release/libpgp_ffi.a

include $(PREBUILT_STATIC_LIBRARY)


################################################################################
# main shared library as used from Java (includes the static ones)
################################################################################

include $(CLEAR_VARS)

LOCAL_MODULE     := native-utils

LOCAL_C_INCLUDES := $(JNI_DIR)/utils/ \
$(JNI_DIR)/messenger-backend/libs/openssl/include \
$(JNI_DIR)/messenger-backend/libs/libetpan/include \
$(JNI_DIR)/messenger-backend/libs/netpgp/include \
$(JNI_DIR)/messenger-backend/libs/sqlite

LOCAL_LDLIBS 	:= -ljnigraphics -llog -lz -latomic
LOCAL_STATIC_LIBRARIES :=  etpan sasl2 sqlite crypto libiconv librpgp
# if you get "undefined reference" errors, the reason for this may be the _order_! Eg. libiconv as the first library does not work!
# "breakpad" was placed after "crypto", NativeLoader.cpp after dc_wrapper.c

LOCAL_CFLAGS 	:= -w -Os -DNULL=0 -DSOCKLEN_T=socklen_t -DLOCALE_NOT_USED -D_LARGEFILE_SOURCE=1 -D_FILE_OFFSET_BITS=64
LOCAL_CFLAGS 	+= -Drestrict='' -D__EMX__ -DOPUS_BUILD -DFIXED_POINT -DUSE_ALLOCA -DHAVE_LRINT -DHAVE_LRINTF -fno-math-errno -std=c99
LOCAL_CFLAGS 	+= -DANDROID_NDK -DDISABLE_IMPORTGL -fno-strict-aliasing -fprefetch-loop-arrays -DAVOID_TABLES -DANDROID_TILE_BASED_DECODE -DANDROID_ARMV6_IDCT -ffast-math -D__STDC_CONSTANT_MACROS
LOCAL_CFLAGS    += -DDC_USE_RPGP
LOCAL_C_INCLUDES += $(JNI_DIR)/../../rpgp/pgp-ffi/

LOCAL_SRC_FILES := \
utils/org_thoughtcrime_securesms_util_FileUtils.cpp \
messenger-backend/libs/netpgp/src/compress.c \
messenger-backend/libs/netpgp/src/create.c \
messenger-backend/libs/netpgp/src/crypto.c \
messenger-backend/libs/netpgp/src/keyring.c \
messenger-backend/libs/netpgp/src/misc.c \
messenger-backend/libs/netpgp/src/openssl_crypto.c \
messenger-backend/libs/netpgp/src/packet-parse.c \
messenger-backend/libs/netpgp/src/packet-show.c \
messenger-backend/libs/netpgp/src/reader.c \
messenger-backend/libs/netpgp/src/signature.c \
messenger-backend/libs/netpgp/src/symmetric.c \
messenger-backend/libs/netpgp/src/validate.c \
messenger-backend/libs/netpgp/src/writer.c \
messenger-backend/src/dc_aheader.c \
messenger-backend/src/dc_apeerstate.c \
messenger-backend/src/dc_array.c \
messenger-backend/src/dc_chat.c \
messenger-backend/src/dc_chatlist.c \
messenger-backend/src/dc_contact.c \
messenger-backend/src/dc_dehtml.c \
messenger-backend/src/dc_hash.c \
messenger-backend/src/dc_imap.c \
messenger-backend/src/dc_oauth2.c \
messenger-backend/src/dc_job.c \
messenger-backend/src/dc_jobthread.c \
messenger-backend/src/dc_key.c \
messenger-backend/src/dc_keyring.c \
messenger-backend/src/dc_loginparam.c \
messenger-backend/src/dc_lot.c \
messenger-backend/src/dc_move.c \
messenger-backend/src/dc_location.c \
messenger-backend/src/dc_context.c \
messenger-backend/src/dc_configure.c \
messenger-backend/src/dc_e2ee.c \
messenger-backend/src/dc_imex.c \
messenger-backend/src/dc_keyhistory.c \
messenger-backend/src/dc_log.c \
messenger-backend/src/dc_openssl.c \
messenger-backend/src/dc_qr.c \
messenger-backend/src/dc_receive_imf.c \
messenger-backend/src/dc_securejoin.c \
messenger-backend/src/dc_mimefactory.c \
messenger-backend/src/dc_mimeparser.c \
messenger-backend/src/dc_msg.c \
messenger-backend/src/dc_param.c \
messenger-backend/src/dc_pgp.c \
messenger-backend/src/dc_saxparser.c \
messenger-backend/src/dc_jsmn.c \
messenger-backend/src/dc_simplify.c \
messenger-backend/src/dc_smtp.c \
messenger-backend/src/dc_sqlite3.c \
messenger-backend/src/dc_stock.c \
messenger-backend/src/dc_strbuilder.c \
messenger-backend/src/dc_strencode.c \
messenger-backend/src/dc_token.c \
messenger-backend/src/dc_tools.c \
dc_wrapper.c

include $(BUILD_SHARED_LIBRARY)
