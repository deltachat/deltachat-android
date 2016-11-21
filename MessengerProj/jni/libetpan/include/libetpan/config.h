/* config.h.  Generated from config.h.in by configure.  */
/* config.h.in.  Generated from configure.ac by autoheader.  */

/* Check for Linux's /usr/include/features.h
 */
#ifdef _FEATURES_H
#  error  config.h must be first file included
#endif

/* Define to detected Berkeley DB major version number */
/* #undef DBVERS */

/* Define to 1 if you have the <arpa/inet.h> header file. */
#define HAVE_ARPA_INET_H 1

/* Define to 1 if you have the <ctype.h> header file. */
#define HAVE_CTYPE_H 1

/* Define to use curl */
/* #undef HAVE_CURL */

/* Define to 1 if you have the <dlfcn.h> header file. */
#define HAVE_DLFCN_H 1

/* Define to use expat */
/* #undef HAVE_EXPAT */

/* Define to 1 if you have the <fcntl.h> header file. */
#define HAVE_FCNTL_H 1

/* Define to use getopt_long */
#define HAVE_GETOPT_LONG 1

/* Define to 1 if you have the `getpagesize' function. */
#define HAVE_GETPAGESIZE 1

/* Define if you have the iconv() function. */
/* #undef HAVE_ICONV */

/* prototype of iconv() has const parameters */
/* #undef HAVE_ICONV_PROTO_CONST */

/* Define to 1 if you have the <inttypes.h> header file. */
#define HAVE_INTTYPES_H 1

/* Define to enable IPv6 support. */
#define HAVE_IPV6 1

/* Define to 1 if you have the `lockfile' library (-llockfile). */
/* #undef HAVE_LIBLOCKFILE */

/* Define to 1 if you have the `nsl' library (-lnsl). */
/* #undef HAVE_LIBNSL */

/* Define to 1 if you have the `socket' library (-lsocket). */
/* #undef HAVE_LIBSOCKET */

/* Define to 1 if you have the <limits.h> header file. */
#define HAVE_LIMITS_H 1

/* Define to 1 if you have the <memory.h> header file. */
#define HAVE_MEMORY_H 1

/* Defined if we run on a W32 API based system */
/* #undef HAVE_MINGW32_SYSTEM */

/* Define to 1 if you have a working `mmap' system call. */
#define HAVE_MMAP 1

/* Define to 1 if you have the <netdb.h> header file. */
/* #undef HAVE_NETDB_H */

/* Define to 1 if you have the <netinet/in.h> header file. */
#define HAVE_NETINET_IN_H 1

/* Define to 1 if you have the <pthread.h> header file. */
#define HAVE_PTHREAD_H 1

/* Define to use setenv */
#define HAVE_SETENV 1

/* Define to 1 if you have the <stdint.h> header file. */
#define HAVE_STDINT_H 1

/* Define to 1 if you have the <stdlib.h> header file. */
#define HAVE_STDLIB_H 1

/* Define to 1 if you have the <strings.h> header file. */
#define HAVE_STRINGS_H 1

/* Define to 1 if you have the <string.h> header file. */
#define HAVE_STRING_H 1

/* Define to 1 if you have the <sys/mman.h> header file. */
#define HAVE_SYS_MMAN_H 1

/* Define to 1 if you have the <sys/param.h> header file. */
#define HAVE_SYS_PARAM_H 1

/* Define to 1 if you have the <sys/select.h> header file. */
#define HAVE_SYS_SELECT_H 1

/* Define to 1 if you have the <sys/socket.h> header file. */
#define HAVE_SYS_SOCKET_H 1

/* Define to 1 if you have the <sys/stat.h> header file. */
#define HAVE_SYS_STAT_H 1

/* Define to 1 if you have the <sys/types.h> header file. */
#define HAVE_SYS_TYPES_H 1

/* Define to 1 if you have the <unistd.h> header file. */
#define HAVE_UNISTD_H 1

/* Define to 1 if you have the <winsock2.h> header file. */
/* #undef HAVE_WINSOCK2_H */

/* Enable classes using zlib compression. */
#define HAVE_ZLIB 1

/* Define to include multithreading support */
#define LIBETPAN_REENTRANT 1

/* Define this to the version of libEtPan */
#define LIBETPAN_VERSION "1.2-dev-20141203"

/* Define this to the major version of libEtPan */
#define LIBETPAN_VERSION_MAJOR 1

/* Define this to the minor version of libEtPan */
#define LIBETPAN_VERSION_MINOR 2

/* Define to the sub-directory in which libtool stores uninstalled libraries.
   */
#define LT_OBJDIR ".libs/"

/* Name of package */
#define PACKAGE "libetpan"

/* Define to the address where bug reports for this package should be sent. */
#define PACKAGE_BUGREPORT "libetpan-devel@lists.sourceforge.net"

/* Define to the full name of this package. */
#define PACKAGE_NAME "libetpan"

/* Define to the full name and version of this package. */
#define PACKAGE_STRING "libetpan 1.2"

/* Define to the one symbol short name of this package. */
#define PACKAGE_TARNAME "libetpan"

/* Define to the home page for this package. */
#define PACKAGE_URL ""

/* Define to the version of this package. */
#define PACKAGE_VERSION "1.2"

/* Define to 1 if you have the ANSI C header files. */
#define STDC_HEADERS 1

/* Define to be lazy on protocol syntax */
#define UNSTRICT_SYNTAX 1

/* Define to use GnuTLS */
/* #undef USE_GNUTLS */

/* Define to use SASL */
#define USE_SASL 1

/* Define to use OpenSSL */
#define USE_SSL 1

/* Version number of package */
#define VERSION "1.2"

/* Define to `__inline__' or `__inline' if that's what the C compiler
   calls it, or to nothing if 'inline' is not supported under any name.  */
#ifndef __cplusplus
/* #undef inline */
#endif
