package ratelimit.storage;

import com.google.gson.Gson;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import ratelimit.common.RateLimitWindowData;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class RedisRateLimitStorage implements RateLimitStorage {
    private static final String PREFIX = "rate_limit_";
    private final RedissonClient redissonClient;

    public RedisRateLimitStorage(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public Set<RateLimitWindowData> getRateLimitWindowData(String key) {
        RMap<String, String> map = redissonClient.getMap(PREFIX + key);
        Collection<String> values = map.values();
        return values.stream().map(str -> new Gson().fromJson(str, RateLimitWindowData.class))
                .collect(Collectors.toSet());
    }

    @Override
    public void storeRateLimitWindowData(String key, Set<RateLimitWindowData> data) {
        RMap<String, String> map = redissonClient.getMap(PREFIX + key);
        for (RateLimitWindowData d : data) {
            map.fastPut(String.format("%s:%s", d.getLimit(), d.getDurationMilliSeconds()), new Gson().toJson(d));
        }

        setTtl(data, map);
    }

    /*
        Set ttl 2 * duration of max window
     */
    private void setTtl(Set<RateLimitWindowData> data, RMap<String, String> map) {
        data.stream().mapToLong(d -> d.getDurationMilliSeconds()).max().ifPresent(maxWindowDuration -> {
            map.clearExpire();
            map.expire(maxWindowDuration * 2, TimeUnit.MILLISECONDS);
        });
    }


}
