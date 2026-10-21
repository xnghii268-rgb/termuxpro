#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <unistd.h>
#include <dirent.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <limits.h>
#include <stdarg.h>

#define OLD_PREFIX "/data/data/com.termux"
#define OLD_PREFIX_LEN (sizeof(OLD_PREFIX) - 1)
#define NEW_PREFIX "/data/data/com.termuxpro.app"
#define NEW_PREFIX_LEN (sizeof(NEW_PREFIX) - 1)

#define OLD_REL "data/data/com.termux"
#define OLD_REL_LEN (sizeof(OLD_REL) - 1)
#define NEW_REL "data/data/com.termuxpro.app"
#define NEW_REL_LEN (sizeof(NEW_REL) - 1)

static inline const char* rewrite_path(const char* path, char* buf, size_t bufsiz) {
    if (!path) return NULL;
    if (strncmp(path, OLD_PREFIX, OLD_PREFIX_LEN) == 0) {
        int ret = snprintf(buf, bufsiz, "%s%s", NEW_PREFIX, path + OLD_PREFIX_LEN);
        if (ret > 0 && (size_t)ret < bufsiz) {
            return buf;
        }
    } else if (strncmp(path, OLD_REL, OLD_REL_LEN) == 0) {
        int ret = snprintf(buf, bufsiz, "%s%s", NEW_REL, path + OLD_REL_LEN);
        if (ret > 0 && (size_t)ret < bufsiz) {
            return buf;
        }
    }
    return path;
}

int open(const char *pathname, int flags, ...) {
    static int (*real_open)(const char *, int, mode_t) = NULL;
    if (!real_open) real_open = dlsym(RTLD_NEXT, "open");
    char buf[PATH_MAX];
    const char *p = rewrite_path(pathname, buf, sizeof(buf));
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args;
        va_start(args, flags);
        mode = (mode_t)va_arg(args, int);
        va_end(args);
    }
    return real_open ? real_open(p, flags, mode) : -1;
}

int open64(const char *pathname, int flags, ...) {
    static int (*real_open64)(const char *, int, mode_t) = NULL;
    if (!real_open64) real_open64 = dlsym(RTLD_NEXT, "open64");
    if (!real_open64) real_open64 = dlsym(RTLD_NEXT, "open");
    char buf[PATH_MAX];
    const char *p = rewrite_path(pathname, buf, sizeof(buf));
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args;
        va_start(args, flags);
        mode = (mode_t)va_arg(args, int);
        va_end(args);
    }
    return real_open64 ? real_open64(p, flags, mode) : -1;
}

int openat(int dirfd, const char *pathname, int flags, ...) {
    static int (*real_openat)(int, const char *, int, mode_t) = NULL;
    if (!real_openat) real_openat = dlsym(RTLD_NEXT, "openat");
    char buf[PATH_MAX];
    const char *p = rewrite_path(pathname, buf, sizeof(buf));
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args;
        va_start(args, flags);
        mode = (mode_t)va_arg(args, int);
        va_end(args);
    }
    return real_openat ? real_openat(dirfd, p, flags, mode) : -1;
}

int openat64(int dirfd, const char *pathname, int flags, ...) {
    static int (*real_openat64)(int, const char *, int, mode_t) = NULL;
    if (!real_openat64) real_openat64 = dlsym(RTLD_NEXT, "openat64");
    if (!real_openat64) real_openat64 = dlsym(RTLD_NEXT, "openat");
    char buf[PATH_MAX];
    const char *p = rewrite_path(pathname, buf, sizeof(buf));
    mode_t mode = 0;
    if (flags & O_CREAT) {
        va_list args;
        va_start(args, flags);
        mode = (mode_t)va_arg(args, int);
        va_end(args);
    }
    return real_openat64 ? real_openat64(dirfd, p, flags, mode) : -1;
}

FILE *fopen(const char *pathname, const char *mode) {
    static FILE *(*real_fopen)(const char *, const char *) = NULL;
    if (!real_fopen) real_fopen = dlsym(RTLD_NEXT, "fopen");
    char buf[PATH_MAX];
    return real_fopen ? real_fopen(rewrite_path(pathname, buf, sizeof(buf)), mode) : NULL;
}

FILE *fopen64(const char *pathname, const char *mode) {
    static FILE *(*real_fopen64)(const char *, const char *) = NULL;
    if (!real_fopen64) real_fopen64 = dlsym(RTLD_NEXT, "fopen64");
    if (!real_fopen64) real_fopen64 = dlsym(RTLD_NEXT, "fopen");
    char buf[PATH_MAX];
    return real_fopen64 ? real_fopen64(rewrite_path(pathname, buf, sizeof(buf)), mode) : NULL;
}

int stat(const char *pathname, struct stat *statbuf) {
    static int (*real_stat)(const char *, struct stat *) = NULL;
    if (!real_stat) real_stat = dlsym(RTLD_NEXT, "stat");
    char buf[PATH_MAX];
    return real_stat ? real_stat(rewrite_path(pathname, buf, sizeof(buf)), statbuf) : -1;
}

int lstat(const char *pathname, struct stat *statbuf) {
    static int (*real_lstat)(const char *, struct stat *) = NULL;
    if (!real_lstat) real_lstat = dlsym(RTLD_NEXT, "lstat");
    char buf[PATH_MAX];
    return real_lstat ? real_lstat(rewrite_path(pathname, buf, sizeof(buf)), statbuf) : -1;
}

int fstatat(int dirfd, const char *pathname, struct stat *statbuf, int flags) {
    static int (*real_fstatat)(int, const char *, struct stat *, int) = NULL;
    if (!real_fstatat) real_fstatat = dlsym(RTLD_NEXT, "fstatat");
    char buf[PATH_MAX];
    return real_fstatat ? real_fstatat(dirfd, rewrite_path(pathname, buf, sizeof(buf)), statbuf, flags) : -1;
}

int access(const char *pathname, int mode) {
    static int (*real_access)(const char *, int) = NULL;
    if (!real_access) real_access = dlsym(RTLD_NEXT, "access");
    char buf[PATH_MAX];
    return real_access ? real_access(rewrite_path(pathname, buf, sizeof(buf)), mode) : -1;
}

int faccessat(int dirfd, const char *pathname, int mode, int flags) {
    static int (*real_faccessat)(int, const char *, int, int) = NULL;
    if (!real_faccessat) real_faccessat = dlsym(RTLD_NEXT, "faccessat");
    char buf[PATH_MAX];
    return real_faccessat ? real_faccessat(dirfd, rewrite_path(pathname, buf, sizeof(buf)), mode, flags) : -1;
}

DIR *opendir(const char *name) {
    static DIR *(*real_opendir)(const char *) = NULL;
    if (!real_opendir) real_opendir = dlsym(RTLD_NEXT, "opendir");
    char buf[PATH_MAX];
    return real_opendir ? real_opendir(rewrite_path(name, buf, sizeof(buf))) : NULL;
}

ssize_t readlink(const char *pathname, char *buf, size_t bufsiz) {
    static ssize_t (*real_readlink)(const char *, char *, size_t) = NULL;
    if (!real_readlink) real_readlink = dlsym(RTLD_NEXT, "readlink");
    char pbuf[PATH_MAX];
    return real_readlink ? real_readlink(rewrite_path(pathname, pbuf, sizeof(pbuf)), buf, bufsiz) : -1;
}

ssize_t readlinkat(int dirfd, const char *pathname, char *buf, size_t bufsiz) {
    static ssize_t (*real_readlinkat)(int, const char *, char *, size_t) = NULL;
    if (!real_readlinkat) real_readlinkat = dlsym(RTLD_NEXT, "readlinkat");
    char pbuf[PATH_MAX];
    return real_readlinkat ? real_readlinkat(dirfd, rewrite_path(pathname, pbuf, sizeof(pbuf)), buf, bufsiz) : -1;
}

int execve(const char *pathname, char *const argv[], char *const envp[]) {
    static int (*real_execve)(const char *, char *const[], char *const[]) = NULL;
    if (!real_execve) real_execve = dlsym(RTLD_NEXT, "execve");
    char buf[PATH_MAX];
    return real_execve ? real_execve(rewrite_path(pathname, buf, sizeof(buf)), argv, envp) : -1;
}

int execv(const char *pathname, char *const argv[]) {
    static int (*real_execv)(const char *, char *const[]) = NULL;
    if (!real_execv) real_execv = dlsym(RTLD_NEXT, "execv");
    char buf[PATH_MAX];
    return real_execv ? real_execv(rewrite_path(pathname, buf, sizeof(buf)), argv) : -1;
}

int execvp(const char *file, char *const argv[]) {
    static int (*real_execvp)(const char *, char *const[]) = NULL;
    if (!real_execvp) real_execvp = dlsym(RTLD_NEXT, "execvp");
    char buf[PATH_MAX];
    return real_execvp ? real_execvp(rewrite_path(file, buf, sizeof(buf)), argv) : -1;
}

int rename(const char *oldpath, const char *newpath) {
    static int (*real_rename)(const char *, const char *) = NULL;
    if (!real_rename) real_rename = dlsym(RTLD_NEXT, "rename");
    char buf1[PATH_MAX], buf2[PATH_MAX];
    return real_rename ? real_rename(rewrite_path(oldpath, buf1, sizeof(buf1)), rewrite_path(newpath, buf2, sizeof(buf2))) : -1;
}

int renameat(int olddirfd, const char *oldpath, int newdirfd, const char *newpath) {
    static int (*real_renameat)(int, const char *, int, const char *) = NULL;
    if (!real_renameat) real_renameat = dlsym(RTLD_NEXT, "renameat");
    char buf1[PATH_MAX], buf2[PATH_MAX];
    return real_renameat ? real_renameat(olddirfd, rewrite_path(oldpath, buf1, sizeof(buf1)), newdirfd, rewrite_path(newpath, buf2, sizeof(buf2))) : -1;
}

int unlink(const char *pathname) {
    static int (*real_unlink)(const char *) = NULL;
    if (!real_unlink) real_unlink = dlsym(RTLD_NEXT, "unlink");
    char buf[PATH_MAX];
    return real_unlink ? real_unlink(rewrite_path(pathname, buf, sizeof(buf))) : -1;
}

int unlinkat(int dirfd, const char *pathname, int flags) {
    static int (*real_unlinkat)(int, const char *, int) = NULL;
    if (!real_unlinkat) real_unlinkat = dlsym(RTLD_NEXT, "unlinkat");
    char buf[PATH_MAX];
    return real_unlinkat ? real_unlinkat(dirfd, rewrite_path(pathname, buf, sizeof(buf)), flags) : -1;
}

int mkdir(const char *pathname, mode_t mode) {
    static int (*real_mkdir)(const char *, mode_t) = NULL;
    if (!real_mkdir) real_mkdir = dlsym(RTLD_NEXT, "mkdir");
    char buf[PATH_MAX];
    return real_mkdir ? real_mkdir(rewrite_path(pathname, buf, sizeof(buf)), mode) : -1;
}

int mkdirat(int dirfd, const char *pathname, mode_t mode) {
    static int (*real_mkdirat)(int, const char *, mode_t) = NULL;
    if (!real_mkdirat) real_mkdirat = dlsym(RTLD_NEXT, "mkdirat");
    char buf[PATH_MAX];
    return real_mkdirat ? real_mkdirat(dirfd, rewrite_path(pathname, buf, sizeof(buf)), mode) : -1;
}

int rmdir(const char *pathname) {
    static int (*real_rmdir)(const char *) = NULL;
    if (!real_rmdir) real_rmdir = dlsym(RTLD_NEXT, "rmdir");
    char buf[PATH_MAX];
    return real_rmdir ? real_rmdir(rewrite_path(pathname, buf, sizeof(buf))) : -1;
}

int chmod(const char *pathname, mode_t mode) {
    static int (*real_chmod)(const char *, mode_t) = NULL;
    if (!real_chmod) real_chmod = dlsym(RTLD_NEXT, "chmod");
    char buf[PATH_MAX];
    return real_chmod ? real_chmod(rewrite_path(pathname, buf, sizeof(buf)), mode) : -1;
}

int fchmodat(int dirfd, const char *pathname, mode_t mode, int flags) {
    static int (*real_fchmodat)(int, const char *, mode_t, int) = NULL;
    if (!real_fchmodat) real_fchmodat = dlsym(RTLD_NEXT, "fchmodat");
    char buf[PATH_MAX];
    return real_fchmodat ? real_fchmodat(dirfd, rewrite_path(pathname, buf, sizeof(buf)), mode, flags) : -1;
}

int symlink(const char *target, const char *linkpath) {
    static int (*real_symlink)(const char *, const char *) = NULL;
    if (!real_symlink) real_symlink = dlsym(RTLD_NEXT, "symlink");
    char buf1[PATH_MAX], buf2[PATH_MAX];
    return real_symlink ? real_symlink(rewrite_path(target, buf1, sizeof(buf1)), rewrite_path(linkpath, buf2, sizeof(buf2))) : -1;
}

int symlinkat(const char *target, int newdirfd, const char *linkpath) {
    static int (*real_symlinkat)(const char *, int, const char *) = NULL;
    if (!real_symlinkat) real_symlinkat = dlsym(RTLD_NEXT, "symlinkat");
    char buf1[PATH_MAX], buf2[PATH_MAX];
    return real_symlinkat ? real_symlinkat(rewrite_path(target, buf1, sizeof(buf1)), newdirfd, rewrite_path(linkpath, buf2, sizeof(buf2))) : -1;
}

int scandir(const char *dirp, struct dirent ***namelist,
            int (*filter)(const struct dirent *),
            int (*compar)(const struct dirent **, const struct dirent **)) {
    static int (*real_scandir)(const char *, struct dirent ***,
                               int (*)(const struct dirent *),
                               int (*)(const struct dirent **, const struct dirent **)) = NULL;
    if (!real_scandir) real_scandir = dlsym(RTLD_NEXT, "scandir");
    char buf[PATH_MAX];
    return real_scandir ? real_scandir(rewrite_path(dirp, buf, sizeof(buf)), namelist, filter, compar) : -1;
}

int scandir64(const char *dirp, struct dirent64 ***namelist,
              int (*filter)(const struct dirent64 *),
              int (*compar)(const struct dirent64 **, const struct dirent64 **)) {
    static int (*real_scandir64)(const char *, struct dirent64 ***,
                                 int (*)(const struct dirent64 *),
                                 int (*)(const struct dirent64 **, const struct dirent64 **)) = NULL;
    if (!real_scandir64) real_scandir64 = dlsym(RTLD_NEXT, "scandir64");
    if (!real_scandir64) real_scandir64 = (int (*)(const char *, struct dirent64 ***, int (*)(const struct dirent64 *), int (*)(const struct dirent64 **, const struct dirent64 **)))dlsym(RTLD_NEXT, "scandir");
    char buf[PATH_MAX];
    return real_scandir64 ? real_scandir64(rewrite_path(dirp, buf, sizeof(buf)), namelist, filter, compar) : -1;
}

char *realpath(const char *path, char *resolved_path) {
    static char *(*real_realpath)(const char *, char *) = NULL;
    if (!real_realpath) real_realpath = dlsym(RTLD_NEXT, "realpath");
    char buf[PATH_MAX];
    return real_realpath ? real_realpath(rewrite_path(path, buf, sizeof(buf)), resolved_path) : NULL;
}

