#include <stdio.h>
#include <stdarg.h>

int __android_log_print(int prio, const char *tag, const char *fmt, ...)
{
    (void)prio;
    (void)tag;
    va_list args;
    va_start(args, fmt);
    int ret = vfprintf(stderr, fmt, args);
    va_end(args);
    return ret;
}
