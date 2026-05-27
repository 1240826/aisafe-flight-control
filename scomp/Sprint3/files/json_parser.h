#ifndef JSON_PARSER_H
#define JSON_PARSER_H

#include "common.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

static const char *jp_skip(const char *p) {
    while (*p && isspace((unsigned char)*p)) p++;
    return p;
}

static const char *jp_close(const char *p, char open, char close) {
    int depth = 0;
    while (*p) {
        if (*p == open) depth++;
        if (*p == close) { depth--; if (depth == 0) return p; }
        p++;
    }
    return p;
}

static int jp_dbl(const char *src, const char *key, double *out) {
    const char *p = strstr(src, key);
    char *end;
    if (!p) return 0;
    p = jp_skip(p + strlen(key));
    *out = strtod(p, &end);
    return end != p;
}

static int jp_str(const char *src, const char *key, char *out, int maxlen) {
    int i = 0;
    const char *p = strstr(src, key);
    if (!p) return 0;
    p = jp_skip(p + strlen(key));
    if (*p != '"') return 0;
    p++;
    while (*p && *p != '"' && i < maxlen - 1) out[i++] = *p++;
    out[i] = '\0';
    return 1;
}

static char *jp_arr(const char *src, const char *key) {
    char *buf;
    int len;
    const char *p = strstr(src, key);
    if (!p) return NULL;
    p = strchr(p, '[');
    if (!p) return NULL;
    const char *e = jp_close(p, '[', ']');
    len = (int)(e - p + 1);
    buf = malloc(len + 1);
    if (!buf) return NULL;
    memcpy(buf, p, len); buf[len] = '\0';
    return buf;
}

#endif
