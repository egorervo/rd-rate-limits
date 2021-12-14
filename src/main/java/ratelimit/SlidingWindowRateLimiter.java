package ratelimit;

import com.antkorwin.xsync.XSync;
import common.LimitRule;
import common.RateLimitWindowData;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class SlidingWindowRateLimiter implements RateLimiter {
    private static XSync<String> XSYNC = new XSync<>();
    private final Set<LimitRule> rules;
    private final Map<String, Set<RateLimitWindowData>> windowDataMap = new HashMap<>();

    public SlidingWindowRateLimiter(Set<LimitRule> rules) {
        this.rules = rules;
        validateRules();
    }

    @Override
    public boolean isLimitNotExceeded(String key) {
        return isLimitNotExceeded(key, 1);
    }

    @Override
    public boolean isLimitNotExceeded(String key, int delta) {
        final long now = System.currentTimeMillis();
        return XSYNC.evaluate(key, () -> evaluateIsLimitNotExceeded(key, delta, now));
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
            windowDataMap.put(key, rateLimitWindowDatas);
            return true;
        }

        return false;
    }

    private boolean isWindowActual(long now, RateLimitWindowData rateLimitWindowData) {
        return now - rateLimitWindowData.getWindowStartTime() < rateLimitWindowData.getDurationMilliSeconds();
    }

    private Set<RateLimitWindowData> getRateLimitWindowData(String key, long now) {
        Set<RateLimitWindowData> rateLimitWindowDatas = windowDataMap.get(key);
        if (null == rateLimitWindowDatas) {
            rateLimitWindowDatas = this.rules.stream()
                    .map(rule -> RateLimitWindowData.builder()
                            .count(0)
                            .windowStartTime(now)
                            .limit(rule.getLimit())
                            .durationMilliSeconds(rule.getDuration().toMillis())
                            .build())
                    .collect(Collectors.toSet());
            windowDataMap.put(key, rateLimitWindowDatas);
        }
        return rateLimitWindowDatas;
    }

    private void validateRules() {
        if (null == this.rules || 0 == this.rules.size()) {
            throw new IllegalArgumentException("Rules set can not be empty!");
        }
    }
}
