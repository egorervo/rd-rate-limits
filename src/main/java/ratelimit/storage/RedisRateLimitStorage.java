package ratelimit.storage;

import com.google.gson.Gson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import ratelimit.common.RateLimitWindowData;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class RedisRateLimitStorage implements RateLimitStorage {
    private final RedissonClient redissonClient;

    public RedisRateLimitStorage(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Set<RateLimitWindowData> getRateLimitWindowData(String key) {
        RMap<String, String> map = redissonClient.getMap(key);
        Collection<String> values = map.values();
        return values.stream().map(str -> new Gson().fromJson(str, RateLimitWindowData.class))
                .collect(Collectors.toSet());
    }

    @Override
    public void storeRateLimitWindowData(String key, Set<RateLimitWindowData> data) {
        RMap<String, String> map = redissonClient.getMap(key);
        for (RateLimitWindowData d : data) {
            map.fastPut(String.format("%s:%s", d.getLimit(), d.getDurationMilliSeconds()), new Gson().toJson(d));
        }
    }
}
