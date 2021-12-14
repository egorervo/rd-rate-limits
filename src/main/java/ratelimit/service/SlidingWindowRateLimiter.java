package ratelimit.service;

import ratelimit.common.LimitRule;
import ratelimit.common.RateLimitWindowData;
import ratelimit.storage.RateLimitStorage;
import ratelimit.service.synchronization.Synchronizer;

import java.util.Set;
import java.util.stream.Collectors;

public class SlidingWindowRateLimiter implements RateLimiter {
    private final Set<LimitRule> rules;
    private final RateLimitStorage rateLimitStorage;
    private final Synchronizer synchronizer;

    public SlidingWindowRateLimiter(Set<LimitRule> rules,
                                    RateLimitStorage rateLimitStorage,
                                    Synchronizer synchronizer) {
        this.rules = rules;
        this.rateLimitStorage = rateLimitStorage;
        this.synchronizer = synchronizer;
        validateRules();
    }

    @Override
    public boolean isLimitNotExceeded(String key) {
        return isLimitNotExceeded(key, 1);
    }

    @Override
    public boolean isLimitNotExceeded(String key, int delta) {
        final long now = System.currentTimeMillis();
        return this.synchronizer.evaluate(key, () -> evaluateIsLimitNotExceeded(key, delta, now));
    }

    private boolean evaluateIsLimitNotExceeded(String key, int delta, long now) {
        Set<RateLimitWindowData> rateLimitWindowDatas = getRateLimitWindowData(key, now);
        boolean notExceeded = true;
        for (RateLimitWindowData rateLimitWindowData : rateLimitWindowDatas) {
            if (!isWindowActual(now, rateLimitWindowData)) {
                rateLimitWindowData.setWindowStartTime(now);
                rateLimitWindowData.setCount(0);
            }
            notExceeded = notExceeded && rateLimitWindowData.getCount() + delta <= rateLimitWindowData.getLimit();
        }

        if (notExceeded) {
            for (RateLimitWindowData rateLimitWindowData : rateLimitWindowDatas) {
                rateLimitWindowData.setCount(rateLimitWindowData.getCount() + delta);
            }
            rateLimitStorage.storeRateLimitWindowData(key, rateLimitWindowDatas);
            return true;
        }

        return false;
    }

    private boolean isWindowActual(long now, RateLimitWindowData rateLimitWindowData) {
        return now - rateLimitWindowData.getWindowStartTime() < rateLimitWindowData.getDurationMilliSeconds();
    }

    private Set<RateLimitWindowData> getRateLimitWindowData(String key, long now) {
        Set<RateLimitWindowData> rateLimitWindowDatas = rateLimitStorage.getRateLimitWindowData(key);
        if (null == rateLimitWindowDatas) {
            rateLimitWindowDatas = this.rules.stream()
                    .map(rule -> RateLimitWindowData.builder()
                            .count(0)
                            .windowStartTime(now)
                            .limit(rule.getLimit())
                            .durationMilliSeconds(rule.getDuration().toMillis())
                            .build())
                    .collect(Collectors.toSet());
            rateLimitStorage.storeRateLimitWindowData(key, rateLimitWindowDatas);
        }
        return rateLimitWindowDatas;
    }

    private void validateRules() {
        if (null == this.rules || 0 == this.rules.size()) {
            throw new IllegalArgumentException("Rules set can not be empty!");
        }
    }
}
