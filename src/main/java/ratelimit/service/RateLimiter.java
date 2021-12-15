package ratelimit.service;

public interface RateLimiter {

    boolean checkLimitExceededAndIncrement(String key);

    boolean checkLimitExceededAndIncrement(String key, int delta);
}
