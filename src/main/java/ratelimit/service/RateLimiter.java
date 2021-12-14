package ratelimit.service;

public interface RateLimiter {

    boolean isLimitNotExceeded(String key);

    boolean isLimitNotExceeded(String key, int delta);
}
