# Makefile.am for the SASL plugins
# Rob Siemborski
# Rob Earhart
# $Id: Makefile.am,v 1.86 2011/09/05 14:18:10 murch Exp $
#
################################################################
# Copyright (c) 2000 Carnegie Mellon University.  All rights reserved.
#
# Redistribution and use in source and binary forms, with or without
# modification, are permitted provided that the following conditions
# are met:
#
# 1. Redistributions of source code must retain the above copyright
#    notice, this list of conditions and the following disclaimer. 
#
# 2. Redistributions in binary form must reproduce the above copyright
#    notice, this list of conditions and the following disclaimer in
#    the documentation and/or other materials provided with the
#    distribution.
#
# 3. The name "Carnegie Mellon University" must not be used to
#    endorse or promote products derived from this software without
#    prior written permission. For permission or any other legal
#    details, please contact  
#      Office of Technology Transfer
#      Carnegie Mellon University
#      5000 Forbes Avenue
#      Pittsburgh, PA  15213-3890
#      (412) 268-4387, fax: (412) 268-7395
#      tech-transfer@andrew.cmu.edu
#
# 4. Redistributions of any form whatsoever must retain the following
#    acknowledgment:
#    "This product includes software developed by Computing Services
#     at Carnegie Mellon University (http://www.cmu.edu/computing/)."
#
# CARNEGIE MELLON UNIVERSITY DISCLAIMS ALL WARRANTIES WITH REGARD TO
# THIS SOFTWARE, INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
# AND FITNESS, IN NO EVENT SHALL CARNEGIE MELLON UNIVERSITY BE LIABLE
# FOR ANY SPECIAL, INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
# WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN
# AN ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING
# OUT OF OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
#
################################################################

# Library version info - here at the top, for sanity
# See <http://www.gnu.org/software/libtool/manual/libtool.html#Versioning>
# CURRENT:REVISION:AGE
plugin_version = 3:0:0

INCLUDES=-I$(top_srcdir)/include -I$(top_srcdir)/lib -I$(top_srcdir)/sasldb -I$(top_builddir)/include
AM_LDFLAGS = -module -export-dynamic -rpath $(plugindir) -version-info $(plugin_version)

COMPAT_OBJS = @LTGETADDRINFOOBJS@ @LTGETNAMEINFOOBJS@ @LTSNPRINTFOBJS@

EXTRA_DIST = makeinit.sh NTMakefile
noinst_SCRIPTS = makeinit.sh

LIB_MYSQL = @LIB_MYSQL@

plugindir = @plugindir@

common_sources = plugin_common.c plugin_common.h

sasldir = $(prefix)/lib/sasl2
sasl_LTLIBRARIES = @SASL_MECHS@
EXTRA_LTLIBRARIES = libplain.la libanonymous.la libkerberos4.la libcrammd5.la \
	libgs2.la libgssapiv2.la libdigestmd5.la liblogin.la libsrp.la libotp.la \
	libscram.la libntlm.la libpassdss.la libsasldb.la libsql.la libldapdb.la

libplain_la_SOURCES = plain.c plain_init.c $(common_sources)
libplain_la_DEPENDENCIES = $(COMPAT_OBJS)
libplain_la_LIBADD = $(PLAIN_LIBS) $(COMPAT_OBJS)

libanonymous_la_SOURCES = anonymous.c anonymous_init.c $(common_sources)
libanonymous_la_DEPENDENCIES = $(COMPAT_OBJS)
libanonymous_la_LIBADD = $(COMPAT_OBJS)

libkerberos4_la_SOURCES = kerberos4.c kerberos4_init.c $(common_sources)
libkerberos4_la_DEPENDENCIES = $(COMPAT_OBJS)
libkerberos4_la_LIBADD = $(SASL_KRB_LIB) $(LIB_SOCKET) $(COMPAT_OBJS)

libgs2_la_SOURCES = gs2.c gs2_init.c gs2_token.c gs2_token.h $(common_sources)
libgs2_la_DEPENDENCIES = $(COMPAT_OBJS)
libgs2_la_LIBADD = $(GSSAPIBASE_LIBS) $(GSSAPI_LIBS) $(LIB_SOCKET) $(COMPAT_OBJS)

libgssapiv2_la_SOURCES = gssapi.c gssapiv2_init.c $(common_sources)
libgssapiv2_la_DEPENDENCIES = $(COMPAT_OBJS)
libgssapiv2_la_LIBADD = $(GSSAPIBASE_LIBS) $(GSSAPI_LIBS) $(LIB_SOCKET) $(COMPAT_OBJS)

libcrammd5_la_SOURCES = cram.c crammd5_init.c $(common_sources)
libcrammd5_la_DEPENDENCIES = $(COMPAT_OBJS)
libcrammd5_la_LIBADD = $(COMPAT_OBJS)

libdigestmd5_la_SOURCES = digestmd5.c digestmd5_init.c $(common_sources)
libdigestmd5_la_DEPENDENCIES = $(COMPAT_OBJS)
libdigestmd5_la_LIBADD = $(LIB_DES) $(LIB_SOCKET) $(COMPAT_OBJS)

libscram_la_SOURCES = scram.c scram_init.c $(common_sources)
libscram_la_DEPENDENCIES = $(COMPAT_OBJS)
libscram_la_LIBADD = $(SCRAM_LIBS) $(COMPAT_OBJS)

liblogin_la_SOURCES = login.c login_init.c $(common_sources)
liblogin_la_DEPENDENCIES = $(COMPAT_OBJS)
liblogin_la_LIBADD = $(PLAIN_LIBS) $(COMPAT_OBJS)

libsrp_la_SOURCES = srp.c srp_init.c $(common_sources)
libsrp_la_DEPENDENCIES = $(COMPAT_OBJS)
libsrp_la_LIBADD = $(SRP_LIBS) $(COMPAT_OBJS)

libotp_la_SOURCES = otp.c otp_init.c otp.h $(common_sources)
libotp_la_DEPENDENCIES = $(COMPAT_OBJS)
libotp_la_LIBADD = $(OTP_LIBS) $(COMPAT_OBJS)

libntlm_la_SOURCES = ntlm.c ntlm_init.c $(common_sources)
libntlm_la_DEPENDENCIES = $(COMPAT_OBJS)
libntlm_la_LIBADD = $(NTLM_LIBS) $(COMPAT_OBJS)

libpassdss_la_SOURCES = passdss.c passdss_init.c $(common_sources)
libpassdss_la_DEPENDENCIES = $(COMPAT_OBJS)
libpassdss_la_LIBADD = $(PASSDSS_LIBS) $(COMPAT_OBJS)

# Auxprop Plugins
libsasldb_la_SOURCES = sasldb.c sasldb_init.c $(common_sources)
libsasldb_la_DEPENDENCIES = $(COMPAT_OBJS)
libsasldb_la_LIBADD = ../sasldb/libsasldb.la $(SASL_DB_LIB) $(COMPAT_OBJS)

libldapdb_la_SOURCES = ldapdb.c ldapdb_init.c $(common_sources)
libldapdb_la_DEPENDENCIES = $(COMPAT_OBJS)
libldapdb_la_LIBADD = $(LIB_LDAP) $(COMPAT_OBJS)

libsql_la_SOURCES = sql.c sql_init.c $(common_sources)
libsql_la_LDFLAGS = $(LIB_MYSQL) $(LIB_PGSQL) $(LIB_SQLITE) $(LIB_SQLITE3) \
	$(AM_LDFLAGS)
libsql_la_DEPENDENCIES = $(COMPAT_OBJS)
libsql_la_LIBADD = $(COMPAT_OBJS)


# Instructions for making the _init files

init_src=anonymous_init.c crammd5_init.c digestmd5_init.c scram_init.c gs2_init.c gssapiv2_init.c \
kerberos4_init.c login_init.c plain_init.c srp_init.c otp_init.c ntlm_init.c \
passdss_init.c sasldb_init.c sql_init.c ldapdb_init.c


CLEANFILES=$(init_src)

${init_src}: $(srcdir)/makeinit.sh
	$(SHELL) $(srcdir)/makeinit.sh

# Compatibility function build rules (they build in lib/)
$(COMPAT_OBJS):
	rm -f $(COMPAT_OBJS)
	cd ../lib; $(MAKE) $(COMPAT_OBJS)
	for file in $(COMPAT_OBJS); do ln -s ../lib/$$file .; done


