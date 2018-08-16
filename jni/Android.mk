JNI_DIR := $(call my-dir)
LOCAL_PATH := $(call my-dir)

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
# main shared library as used from Java (includes the static ones)
################################################################################

include $(CLEAR_VARS)

LOCAL_MODULE     := native-utils

LOCAL_C_INCLUDES := $(JNI_DIR)/utils/ \
$(JNI_DIR)/openssl/include \
$(JNI_DIR)/messenger-backend/libs/libetpan/include \
$(JNI_DIR)/messenger-backend/libs/netpgp/include \
$(JNI_DIR)/messenger-backend/libs/sqlite

LOCAL_LDLIBS 	:= -ljnigraphics -llog -lz -latomic
LOCAL_STATIC_LIBRARIES :=  etpan sasl2 sqlite crypto libiconv
# if you get "undefined reference" errors, the reason for this may be the _order_! Eg. libiconv as the first library does not work!
# "breakpad" was placed after "crypto", NativeLoader.cpp after mrwrapper.c

LOCAL_CFLAGS 	:= -w -Os -DNULL=0 -DSOCKLEN_T=socklen_t -DLOCALE_NOT_USED -D_LARGEFILE_SOURCE=1 -D_FILE_OFFSET_BITS=64
LOCAL_CFLAGS 	+= -Drestrict='' -D__EMX__ -DOPUS_BUILD -DFIXED_POINT -DUSE_ALLOCA -DHAVE_LRINT -DHAVE_LRINTF -fno-math-errno
LOCAL_CFLAGS 	+= -DANDROID_NDK -DDISABLE_IMPORTGL -fno-strict-aliasing -fprefetch-loop-arrays -DAVOID_TABLES -DANDROID_TILE_BASED_DECODE -DANDROID_ARMV6_IDCT -ffast-math -D__STDC_CONSTANT_MACROS

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
messenger-backend/src/dc_job.c \
messenger-backend/src/dc_key.c \
messenger-backend/src/dc_keyring.c \
messenger-backend/src/dc_loginparam.c \
messenger-backend/src/dc_lot.c \
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
messenger-backend/src/dc_simplify.c \
messenger-backend/src/dc_smtp.c \
messenger-backend/src/dc_sqlite3.c \
messenger-backend/src/dc_stock.c \
messenger-backend/src/dc_strbuilder.c \
messenger-backend/src/dc_strencode.c \
messenger-backend/src/dc_token.c \
messenger-backend/src/dc_tools.c \
messenger-backend/src/dc_uudecode.c \
messenger-backend/cmdline/cmdline.c \
mrwrapper.c

include $(BUILD_SHARED_LIBRARY)
