#include "us110_weather.h"
#include "json_parser.h"
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <math.h>

static double jp_dbl_def(const char *src, const char *key, double def)
{
    double v;
    return jp_dbl(src, key, &v) ? v : def;
}

int load_weather_from_json(const char *path, WeatherDataSet *wds)
{
    FILE *f;
    char *buf;
    long sz;

    memset(wds, 0, sizeof(WeatherDataSet));

    f = fopen(path, "r");
    if (!f) { perror("fopen weather json"); return -1; }
    fseek(f, 0, SEEK_END);
    sz = ftell(f);
    rewind(f);
    buf = malloc(sz + 1);
    if (!buf) { fclose(f); return -1; }
    fread(buf, 1, sz, f);
    buf[sz] = '\0';
    fclose(f);

    if (!jp_str(buf, "\"provider\":", wds->provider, sizeof(wds->provider)))
        jp_str(buf, "\"Provider\":", wds->provider, sizeof(wds->provider));
    wds->duration_hours = jp_dbl_def(buf, "\"duration_hours\":", 0.0);

    char *arr = jp_arr(buf, "\"zones\":");
    if (!arr) { free(buf); return -1; }

    const char *p = arr;
    int n = 0;
    while (n < MAX_WEATHER_ZONES) {
        while (*p && *p != '{' && *p != ']') p++;
        if (!*p || *p == ']') break;
        const char *e = jp_close(p, '{', '}');
        int len = (int)(e - p + 1);
        char *obj = malloc(len + 1);
        if (!obj) break;
        memcpy(obj, p, len); obj[len] = '\0';

        WeatherZone *z = &wds->zones[n];
        z->lat_north = jp_dbl_def(obj, "\"lat_north\":",
            jp_dbl_def(obj, "\"lat1\":", 0.0));
        z->lat_south = jp_dbl_def(obj, "\"lat_south\":",
            jp_dbl_def(obj, "\"lat2\":", 0.0));
        z->lon_west = jp_dbl_def(obj, "\"lon_west\":",
            jp_dbl_def(obj, "\"lon1\":", 0.0));
        z->lon_east = jp_dbl_def(obj, "\"lon_east\":",
            jp_dbl_def(obj, "\"lon2\":", 0.0));
        z->alt_ft_lo = jp_dbl_def(obj, "\"alt_ft_lo\":", 0.0);
        z->alt_ft_hi = jp_dbl_def(obj, "\"alt_ft_hi\":", 0.0);
        z->dir_deg = jp_dbl_def(obj, "\"dir_deg\":", 0.0);
        z->speed_kt = jp_dbl_def(obj, "\"speed_kt\":", 0.0);

        free(obj);
        n++;
        p = e + 1;
    }
    wds->n_zones = n;
    free(arr);
    free(buf);
    return (n > 0) ? 0 : -1;
}

int get_wind_at(WeatherDataSet *wds, double lat, double lon, double alt_m,
                double *speed_kt, double *dir_deg)
{
    for (int i = 0; i < wds->n_zones; i++) {
        WeatherZone *z = &wds->zones[i];
        double alt_ft = alt_m / 0.3048;
        if (lat >= z->lat_south && lat <= z->lat_north &&
            lon >= z->lon_west && lon <= z->lon_east &&
            alt_ft >= z->alt_ft_lo && alt_ft <= z->alt_ft_hi) {
            *speed_kt = z->speed_kt;
            *dir_deg = z->dir_deg;
            return 1;
        }
    }
    return 0;
}
