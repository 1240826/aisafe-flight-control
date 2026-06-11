package eapli.aisafe.ui.jfx.util;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

public final class DataCache {

    private static final long TTL_MILLIS = 5 * 60 * 1000L;
    private static final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    private DataCache() {
    }

    @SuppressWarnings("unchecked")
    public static <T> List<T> getOrLoad(final String key, final Supplier<List<T>> loader) {
        final CacheEntry existing = cache.get(key);
        if (existing != null && !existing.isExpired()) {
            return (List<T>) existing.data;
        }
        final List<T> data = loader.get();
        cache.put(key, new CacheEntry(data, System.currentTimeMillis()));
        return data;
    }

    public static void clear() {
        cache.clear();
    }

    public static void invalidate(final String key) {
        cache.remove(key);
    }

    private static final class CacheEntry {
        private final List<?> data;
        private final long timestamp;

        CacheEntry(final List<?> data, final long timestamp) {
            this.data = data;
            this.timestamp = timestamp;
        }

        boolean isExpired() {
            return System.currentTimeMillis() - timestamp > TTL_MILLIS;
        }
    }
}
