### Usage

#### In java memory implementation
```java
    RateLimiter rateLimiter = new SlidingWindowRateLimiter(Set.of(
        new LimitRule(Duration.ofSeconds(2), 2),
        new LimitRule(Duration.ofSeconds(10), 5)
    ), new InMemoryRateLimitStorage(), new InMemorySynchronizer());

    boolean accepted = rateLimiter.checkLimitExceededAndIncrement("userId:1");
```

#### In redis
```java
    RedissonClient client = createClient();

    RateLimiter rateLimiter = new SlidingWindowRateLimiter(Set.of(
        new LimitRule(Duration.ofSeconds(2), 2),
        new LimitRule(Duration.ofSeconds(10), 5)
    ), new RedisRateLimitStorage(client), new RedisSynchronizer(client));

    boolean accepted = rateLimiter.checkLimitExceededAndIncrement("userId:1");
```
