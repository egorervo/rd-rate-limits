package ratelimit.storage;

import ratelimit.common.RateLimitWindowData;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryRateLimitStorage implements RateLimitStorage {
    private final Map<String, Set<RateLimitWindowData>> windowDataMap = new ConcurrentHashMap<>();

    @Override
    public Set<RateLimitWindowData> getRateLimitWindowData(String key) {
        return windowDataMap.get(key);
    }

    @Override
    public void storeRateLimitWindowData(String key, Set<RateLimitWindowData> data) {
        windowDataMap.put(key, data);
    }
}
