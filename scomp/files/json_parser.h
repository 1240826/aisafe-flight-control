#ifndef JSON_PARSER_H
#define JSON_PARSER_H

/*
 * Reads the JSON format used in the SCOMP input files.
 * Accepts a JSON array [ {...}, {...} ] or a single object { ... }.
 * No external libraries required.
 */

#include "common.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>

static const char *jp_skip(const char *p)
{
    while (*p && isspace((unsigned char)*p)) p++;
    return p;
}

static const char *jp_close(const char *p, char open, char close)
{
    int depth = 0;
    while (*p) {
        if (*p == open)  depth++;
        if (*p == close) { depth--; if (depth == 0) return p; }
        p++;
    }
    return p;
}

static int jp_dbl(const char *src, const char *key, double *out)
{
    const char *p = strstr(src, key);
    char *end;
    if (!p) return 0;
    p = jp_skip(p + strlen(key));
    *out = strtod(p, &end);
    return end != p;
}

static int jp_str(const char *src, const char *key, char *out, int maxlen)
{
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

static char *jp_obj(const char *src, const char *key)
{
    char *buf;
    int len;
    const char *p = strstr(src, key);
    if (!p) return NULL;
    p = strchr(p, '{');
    if (!p) return NULL;
    const char *e = jp_close(p, '{', '}');
    len = (int)(e - p + 1);
    buf = malloc(len + 1);
    if (!buf) return NULL;
    memcpy(buf, p, len);
    buf[len] = '\0';
    return buf;
}

static char *jp_arr(const char *src, const char *key)
{
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
    memcpy(buf, p, len);
    buf[len] = '\0';
    return buf;
}

static void jp_pos(const char *obj, Pos3D *pos)
{
    double v;
    char *a;
    if (jp_dbl(obj, "\"Latitude\":",  &v)) pos->lat = v;
    if (jp_dbl(obj, "\"Longitude\":", &v)) pos->lon = v;
    a = jp_obj(obj, "\"Altitude\"");
    if (a) {
        if (jp_dbl(a, "\"Quantity\":", &v) || jp_dbl(a, "\"Value\":", &v))
            pos->alt = v;
        free(a);
    }
}

static int jp_profile(const char *arr, ProfileEntry *t, int max, int is_desc)
{
    int n = 0;
    double v;
    const char *p = arr;
    char *buf, *a, *s, *r;

    while (n < max) {
        while (*p && *p != '{' && *p != ']') p++;
        if (!*p || *p == ']') break;
        const char *e = jp_close(p, '{', '}');
        int len = (int)(e - p + 1);
        buf = malloc(len + 1);
        if (!buf) break;
        memcpy(buf, p, len); buf[len] = '\0';

        a = jp_obj(buf, "\"Altitude\"");
        if (a) { if (jp_dbl(a, "\"Value\":", &v)) t[n].alt_m = v; free(a); }
        s = jp_obj(buf, "\"Speed\"");
        if (s) { if (jp_dbl(s, "\"Value\":", &v)) t[n].spd_kt = v; free(s); }
        r = jp_obj(buf, is_desc ? "\"RateDescent\"" : "\"RateClimb\"");
        if (r) { if (jp_dbl(r, "\"Value\":", &v)) t[n].rate_ms = v; free(r); }

        free(buf);
        n++;
        p = e + 1;
    }
    return n;
}

static int jp_segments(const char *arr, Segment *segs, int max)
{
    int n = 0;
    char mode[16];
    const char *p = arr;
    char *buf, *so, *eo;

    while (n < max) {
        while (*p && *p != '{' && *p != ']') p++;
        if (!*p || *p == ']') break;
        const char *e = jp_close(p, '{', '}');
        int len = (int)(e - p + 1);
        buf = malloc(len + 1);
        if (!buf) break;
        memcpy(buf, p, len); buf[len] = '\0';

        mode[0] = '\0';
        jp_str(buf, "\"Mode\":", mode, sizeof(mode));
        if      (strcmp(mode, "climb")   == 0) segs[n].phase = CLIMB;
        else if (strcmp(mode, "descend") == 0) segs[n].phase = DESCEND;
        else                                    segs[n].phase = CRUISE;

        so = jp_obj(buf, "\"Start\""); if (so) { jp_pos(so, &segs[n].start); free(so); }
        eo = jp_obj(buf, "\"End\"");   if (eo) { jp_pos(eo, &segs[n].end);   free(eo); }

        free(buf);
        n++;
        p = e + 1;
    }
    return n;
}

static int jp_plan(const char *obj, FlightPlan *fp)
{
    const char *p;
    char *la, *leg, *fo, *ca, *da, *co, *so, *pr;
    double v;
    int ll;

    memset(fp, 0, sizeof(FlightPlan));

    /* ID: integer or quoted string */
    p = strstr(obj, "\"ID\":");
    if (p) {
        p = jp_skip(p + 5);
        if (*p == '"') jp_str(obj, "\"ID\":", fp->id, ID_LEN);
        else { int id = (int)strtol(p, NULL, 10); snprintf(fp->id, ID_LEN, "%d", id); }
    }

    /* First leg only */
    la = jp_arr(obj, "\"Leg\"");
    if (!la) return -1;
    p = la;
    while (*p && *p != '{') p++;
    if (!*p) { free(la); return -1; }
    const char *le = jp_close(p, '{', '}');
    ll = (int)(le - p + 1);
    leg = malloc(ll + 1);
    if (!leg) { free(la); return -1; }
    memcpy(leg, p, ll); leg[ll] = '\0';
    free(la);

    fo = jp_obj(leg, "\"Fuel\"");
    if (fo) { if (jp_dbl(fo, "\"Quantity\":", &v)) fp->fuel_kg = v; free(fo); }

    /* Flight Profile */
    p = strstr(leg, "\"Flight Profile\"");
    if (!p) p = strstr(leg, "\"FlightProfile\"");
    if (p) {
        const char *pb = strchr(p, '{');
        if (pb) {
            const char *pe = jp_close(pb, '{', '}');
            int plen = (int)(pe - pb + 1);
            pr = malloc(plen + 1);
            if (pr) {
                memcpy(pr, pb, plen); pr[plen] = '\0';
                ca = jp_arr(pr, "\"Climb\"");
                if (ca) { fp->n_climb = jp_profile(ca+1, fp->climb, MAX_PROFILE, 0); free(ca); }
                da = jp_arr(pr, "\"Descend\"");
                if (da) { fp->n_desc = jp_profile(da+1, fp->desc, MAX_PROFILE, 1); free(da); }
                co = jp_obj(pr, "\"Cruise\"");
                if (co) {
                    so = jp_obj(co, "\"Speed\"");
                    if (so) { if (jp_dbl(so, "\"Value\":", &v)) fp->cruise_kt = v; free(so); }
                    free(co);
                }
                free(pr);
            }
        }
    }

    ca = jp_arr(leg, "\"Segments\"");
    if (ca) { fp->n_seg = jp_segments(ca+1, fp->seg, MAX_SEGMENTS); free(ca); }

    free(leg);
    return (fp->n_seg > 0) ? 0 : -1;
}

/*
 * load_plans
 *   Read all flight plans from a JSON file.
 *   Accepts array [ {...}, ... ] or single object { ... }.
 *   Returns number loaded, -1 on error.
 */
static int load_plans(const char *path, FlightPlan *plans, int max)
{
    FILE *f;
    char *buf;
    long sz;
    int n = 0;
    const char *p;

    f = fopen(path, "r");
    if (!f) { perror("fopen"); return -1; }
    fseek(f, 0, SEEK_END);
    sz = ftell(f);
    rewind(f);
    buf = malloc(sz + 1);
    if (!buf) { fclose(f); return -1; }
    fread(buf, 1, sz, f);
    buf[sz] = '\0';
    fclose(f);

    p = jp_skip(buf);
    if (*p == '[') {
        p++;
        while (n < max) {
            p = jp_skip(p);
            if (*p == ']' || !*p) break;
            if (*p == ',') { p++; continue; }
            if (*p != '{') { p++; continue; }
            const char *e = jp_close(p, '{', '}');
            int len = (int)(e - p + 1);
            char *obj = malloc(len + 1);
            if (!obj) break;
            memcpy(obj, p, len); obj[len] = '\0';
            if (jp_plan(obj, &plans[n]) == 0) n++;
            else fprintf(stderr, "json: skipping plan #%d\n", n + 1);
            free(obj);
            p = e + 1;
        }
    } else if (*p == '{') {
        if (jp_plan(p, &plans[0]) == 0) n = 1;
    }

    free(buf);
    return n;
}

#endif
