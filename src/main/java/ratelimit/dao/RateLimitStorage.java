package ratelimit.dao;

import ratelimit.common.RateLimitWindowData;

import java.util.Set;

public interface RateLimitStorage {

    Set<RateLimitWindowData> getRateLimitWindowData(String key);

    void storeRateLimitWindowData(String key, Set<RateLimitWindowData> data);
}
