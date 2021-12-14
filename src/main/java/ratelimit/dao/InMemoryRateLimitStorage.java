package ratelimit.dao;

import ratelimit.common.RateLimitWindowData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class InMemoryRateLimitStorage implements RateLimitStorage {
    private final Map<String, Set<RateLimitWindowData>> windowDataMap = new HashMap<>();

    @Override
    public Set<RateLimitWindowData> getRateLimitWindowData(String key) {
        return windowDataMap.get(key);
    }

    @Override
    public void storeRateLimitWindowData(String key, Set<RateLimitWindowData> data) {
        windowDataMap.put(key, data);
    }
}
