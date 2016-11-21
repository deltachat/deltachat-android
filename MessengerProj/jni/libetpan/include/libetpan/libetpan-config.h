#ifndef LIBETPAN_CONFIG_H
#define LIBETPAN_CONFIG_H
#if WIN32
# define MMAP_UNAVAILABLE
#endif
#if defined(_MSC_VER) && !defined(__cplusplus)
# define inline __inline
#endif
#include <limits.h>
#ifndef PATH_MAX
#define PATH_MAX 4096
#endif
#include <sys/param.h>
#include <inttypes.h>
#define MAIL_DIR_SEPARATOR '/'
#define MAIL_DIR_SEPARATOR_S "/"
#ifdef _MSC_VER
# ifdef LIBETPAN_DLL
# define LIBETPAN_EXPORT __declspec(dllexport)
# else
# define LIBETPAN_EXPORT __declspec(dllimport)
# endif
#else
# define LIBETPAN_EXPORT
#endif
#endif
