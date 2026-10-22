#define _GNU_SOURCE
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdbool.h>
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

#define OLD_USER0 "/data/user/0/com.termux"
#define OLD_USER0_LEN (sizeof(OLD_USER0) - 1)
#define NEW_USER0 "/data/user/0/com.termuxpro.app"
#define NEW_USER0_LEN (sizeof(NEW_USER0) - 1)

#define OLD_USER0_REL "data/user/0/com.termux"
#define OLD_USER0_REL_LEN (sizeof(OLD_USER0_REL) - 1)
#define NEW_USER0_REL "data/user/0/com.termuxpro.app"
#define NEW_USER0_REL_LEN (sizeof(NEW_USER0_REL) - 1)

static inline const char* rewrite_path(const char* path, char* buf, size_t bufsiz) {
    if (!path) return NULL;
    if (strncmp(path, OLD_PREFIX, OLD_PREFIX_LEN) == 0) {
        char next = path[OLD_PREFIX_LEN];
        if (next == '/' || next == '\0') {
            int ret = snprintf(buf, bufsiz, "%s%s", NEW_PREFIX, path + OLD_PREFIX_LEN);
            if (ret > 0 && (size_t)ret < bufsiz) {
                return buf;
            }
        }
    } else if (strncmp(path, OLD_REL, OLD_REL_LEN) == 0) {
        char next = path[OLD_REL_LEN];
        if (next == '/' || next == '\0') {
            int ret = snprintf(buf, bufsiz, "%s%s", NEW_REL, path + OLD_REL_LEN);
            if (ret > 0 && (size_t)ret < bufsiz) {
                return buf;
            }
        }
    } else if (strncmp(path, OLD_USER0, OLD_USER0_LEN) == 0) {
        char next = path[OLD_USER0_LEN];
        if (next == '/' || next == '\0') {
            int ret = snprintf(buf, bufsiz, "%s%s", NEW_USER0, path + OLD_USER0_LEN);
            if (ret > 0 && (size_t)ret < bufsiz) {
                return buf;
            }
        }
    } else if (strncmp(path, OLD_USER0_REL, OLD_USER0_REL_LEN) == 0) {
        char next = path[OLD_USER0_REL_LEN];
        if (next == '/' || next == '\0') {
            int ret = snprintf(buf, bufsiz, "%s%s", NEW_USER0_REL, path + OLD_USER0_REL_LEN);
            if (ret > 0 && (size_t)ret < bufsiz) {
                return buf;
            }
        }
    } else if (strncmp(path, "/bin/", 5) == 0) {
        int ret = snprintf(buf, bufsiz, "%s/files/usr/bin/%s", NEW_PREFIX, path + 5);
        if (ret > 0 && (size_t)ret < bufsiz) {
            return buf;
        }
    } else if (strcmp(path, "/bin") == 0) {
        int ret = snprintf(buf, bufsiz, "%s/files/usr/bin", NEW_PREFIX);
        if (ret > 0 && (size_t)ret < bufsiz) {
            return buf;
        }
    } else if (strncmp(path, "/usr/bin/", 9) == 0) {
        int ret = snprintf(buf, bufsiz, "%s/files/usr/bin/%s", NEW_PREFIX, path + 9);
        if (ret > 0 && (size_t)ret < bufsiz) {
            return buf;
        }
    } else if (strcmp(path, "/usr/bin") == 0) {
        int ret = snprintf(buf, bufsiz, "%s/files/usr/bin", NEW_PREFIX);
        if (ret > 0 && (size_t)ret < bufsiz) {
            return buf;
        }
    }
    return path;
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

static inline int handle_shebang_exec(const char *p, char *const argv[], char *const envp[], int (*real_fn)(const char *, char *const[], char *const[])) {
    static int (*real_open_fn)(const char *, int, mode_t) = NULL;
    if (!real_open_fn) real_open_fn = dlsym(RTLD_NEXT, "open");
    int fd = real_open_fn ? real_open_fn(p, O_RDONLY | O_CLOEXEC, 0) : -1;
    if (fd >= 0) {
        char head[512];
        ssize_t n = read(fd, head, sizeof(head) - 1);
        close(fd);
        if (n > 2 && head[0] == '#' && head[1] == '!') {
            head[n] = '\0';
            char *eol = strchr(head, '\n');
            if (eol) *eol = '\0';
            char *interp = head + 2;
            while (*interp == ' ' || *interp == '\t') interp++;
            char *arg = NULL;
            char *sp = strchr(interp, ' ');
            if (sp) {
                *sp = '\0';
                arg = sp + 1;
                while (*arg == ' ' || *arg == '\t') arg++;
                if (*arg == '\0') arg = NULL;
            }
            char interp_buf[PATH_MAX];
            const char *new_interp = rewrite_path(interp, interp_buf, sizeof(interp_buf));
            bool need_rewrite = (new_interp != interp) || (strncmp(interp, "/bin/", 5) == 0) || (strncmp(interp, "/usr/bin/", 9) == 0);
            if (need_rewrite) {
                if (strncmp(interp, "/bin/", 5) == 0) {
                    snprintf(interp_buf, sizeof(interp_buf), "%s/files/usr/bin/%s", NEW_PREFIX, interp + 5);
                    new_interp = interp_buf;
                } else if (strncmp(interp, "/usr/bin/", 9) == 0) {
                    snprintf(interp_buf, sizeof(interp_buf), "%s/files/usr/bin/%s", NEW_PREFIX, interp + 9);
                    new_interp = interp_buf;
                }
                int argc = 0;
                while (argv && argv[argc]) argc++;
                char **new_argv = (char **)malloc((argc + (arg ? 1 : 0) + 4) * sizeof(char *));
                if (new_argv) {
                    int idx = 0;
                    new_argv[idx++] = (char *)new_interp;
                    if (arg) new_argv[idx++] = arg;
                    new_argv[idx++] = (char *)p;
                    for (int i = 1; i < argc; i++) {
                        new_argv[idx++] = argv[i];
                    }
                    new_argv[idx++] = NULL;
                    int ret = real_fn ? real_fn(new_interp, new_argv, envp) : -1;
                    free(new_argv);
                    return ret;
                }
            }
        }
    }
    return real_fn ? real_fn(p, argv, envp) : -1;
}

int execve(const char *pathname, char *const argv[], char *const envp[]) {
    static int (*real_execve)(const char *, char *const[], char *const[]) = NULL;
    if (!real_execve) real_execve = dlsym(RTLD_NEXT, "execve");
    char buf[PATH_MAX];
    const char *p = rewrite_path(pathname, buf, sizeof(buf));
    return handle_shebang_exec(p, argv, envp, real_execve);
}

int execvpe(const char *file, char *const argv[], char *const envp[]) {
    static int (*real_execvpe)(const char *, char *const[], char *const[]) = NULL;
    if (!real_execvpe) real_execvpe = dlsym(RTLD_NEXT, "execvpe");
    char buf[PATH_MAX];
    const char *p = rewrite_path(file, buf, sizeof(buf));
    return handle_shebang_exec(p, argv, envp, real_execvpe);
}

static inline int call_execv(const char *p, char *const argv[], char *const envp[]) {
    (void)envp;
    static int (*real_execv)(const char *, char *const[]) = NULL;
    if (!real_execv) real_execv = dlsym(RTLD_NEXT, "execv");
    return real_execv ? real_execv(p, argv) : -1;
}

static inline int call_execvp(const char *p, char *const argv[], char *const envp[]) {
    (void)envp;
    static int (*real_execvp)(const char *, char *const[]) = NULL;
    if (!real_execvp) real_execvp = dlsym(RTLD_NEXT, "execvp");
    return real_execvp ? real_execvp(p, argv) : -1;
}

int execv(const char *pathname, char *const argv[]) {
    char buf[PATH_MAX];
    const char *p = rewrite_path(pathname, buf, sizeof(buf));
    return handle_shebang_exec(p, argv, NULL, call_execv);
}

int execvp(const char *file, char *const argv[]) {
    char buf[PATH_MAX];
    const char *p = rewrite_path(file, buf, sizeof(buf));
    return handle_shebang_exec(p, argv, NULL, call_execvp);
}

int execl(const char *pathname, const char *arg, ...) {
    if (!arg) {
        char *const argv[] = { NULL };
        return execv(pathname, argv);
    }
    va_list args;
    va_start(args, arg);
    int count = 1;
    while (va_arg(args, const char *)) count++;
    va_end(args);

    char **argv = (char **)malloc((count + 1) * sizeof(char *));
    if (!argv) return -1;
    argv[0] = (char *)arg;
    va_start(args, arg);
    for (int i = 1; i < count; i++) {
        argv[i] = va_arg(args, char *);
    }
    argv[count] = NULL;
    va_end(args);

    int ret = execv(pathname, argv);
    free(argv);
    return ret;
}

int execlp(const char *file, const char *arg, ...) {
    if (!arg) {
        char *const argv[] = { NULL };
        return execvp(file, argv);
    }
    va_list args;
    va_start(args, arg);
    int count = 1;
    while (va_arg(args, const char *)) count++;
    va_end(args);

    char **argv = (char **)malloc((count + 1) * sizeof(char *));
    if (!argv) return -1;
    argv[0] = (char *)arg;
    va_start(args, arg);
    for (int i = 1; i < count; i++) {
        argv[i] = va_arg(args, char *);
    }
    argv[count] = NULL;
    va_end(args);

    int ret = execvp(file, argv);
    free(argv);
    return ret;
}

int execle(const char *pathname, const char *arg, ...) {
    if (!arg) {
        va_list args;
        va_start(args, arg);
        char *const *envp = va_arg(args, char *const *);
        va_end(args);
        char *const argv[] = { NULL };
        return execve(pathname, argv, (char *const *)envp);
    }
    va_list args;
    va_start(args, arg);
    int count = 1;
    while (va_arg(args, const char *)) count++;
    char *const *envp = va_arg(args, char *const *);
    va_end(args);

    char **argv = (char **)malloc((count + 1) * sizeof(char *));
    if (!argv) return -1;
    argv[0] = (char *)arg;
    va_start(args, arg);
    for (int i = 1; i < count; i++) {
        argv[i] = va_arg(args, char *);
    }
    argv[count] = NULL;
    va_end(args);

    int ret = execve(pathname, argv, (char *const *)envp);
    free(argv);
    return ret;
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

int utimensat(int dirfd, const char *pathname, const struct timespec times[2], int flags) {
    static int (*real_utimensat)(int, const char *, const struct timespec[2], int) = NULL;
    if (!real_utimensat) real_utimensat = dlsym(RTLD_NEXT, "utimensat");
    char buf[PATH_MAX];
    return real_utimensat ? real_utimensat(dirfd, rewrite_path(pathname, buf, sizeof(buf)), times, flags) : -1;
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


