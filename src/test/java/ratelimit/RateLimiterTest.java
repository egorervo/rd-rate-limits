package ratelimit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import ratelimit.common.LimitRule;
import ratelimit.service.synchronization.InMemorySynchronizer;
import ratelimit.service.synchronization.RedisSynchronizer;
import ratelimit.storage.InMemoryRateLimitStorage;
import ratelimit.storage.RedisRateLimitStorage;
import redis.embedded.RedisServer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RateLimiterTest {
    public static final int PORT = 6400;
    private static RedisServer redisServer;

    @BeforeAll
    public static void init() {
        redisServer = new RedisServer(PORT);
        redisServer.start();
    }

    @AfterAll
    public static void destroy() {
        redisServer.stop();
    }

    @Test
    public void rateLimitsTest() throws InterruptedException {
        RateLimiter rateLimiter = new SlidingWindowRateLimiter(Set.of(
                new LimitRule(Duration.ofSeconds(2), 2),
                new LimitRule(Duration.ofSeconds(10), 5)
        ), new InMemoryRateLimitStorage(), new InMemorySynchronizer());

        int cyclesToTest = 3;
        doTest(rateLimiter, cyclesToTest);
    }

    @Test
    public void rateLimitsRedisTest() throws InterruptedException {

        RedissonClient client = createClient();
        RateLimiter rateLimiter = new SlidingWindowRateLimiter(Set.of(
                new LimitRule(Duration.ofSeconds(2), 2),
                new LimitRule(Duration.ofSeconds(10), 5)
        ), new RedisRateLimitStorage(client), new RedisSynchronizer(client));

        int cyclesToTest = 3;
        doTest(rateLimiter, cyclesToTest);
    }

    @Test
    public void multiThreadingRateLimitsTest() throws InterruptedException {
        RateLimiter rateLimiter = new SlidingWindowRateLimiter(Set.of(
                new LimitRule(Duration.ofSeconds(10), 100)
        ), new InMemoryRateLimitStorage(), new InMemorySynchronizer());

        doMultiThreadingTest(rateLimiter);
    }

    @Test
    public void multiThreadingRedisRateLimitsTest() throws InterruptedException {

        RedissonClient client = createClient();
        RateLimiter rateLimiter = new SlidingWindowRateLimiter(Set.of(
                new LimitRule(Duration.ofSeconds(10), 100)
        ), new RedisRateLimitStorage(client), new RedisSynchronizer(client));


        doMultiThreadingTest(rateLimiter);
    }

    private void doMultiThreadingTest(RateLimiter rateLimiter) throws InterruptedException {
        final ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<Boolean>> futures = new ArrayList<>();
        for (int i=0; i<100; i++) {
            futures.add(executorService.submit(() -> rateLimiter.checkLimitExceededAndIncrement("127.0.0.1", 1)));
        }
        executorService.shutdown();
        executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        boolean allCallsAllowed = futures.stream().map(f -> {
            try {
                return f.get();
            } catch (Exception e) {
                return false;
            }
        }).reduce((f1, f2) -> f1 && f2).get();

        assertEquals(futures.size(), 100);
        assertTrue(allCallsAllowed);
        assertFalse(rateLimiter.checkLimitExceededAndIncrement("127.0.0.1", 1));
    }

    private void doTest(RateLimiter rateLimiter, int cyclesToTest) throws InterruptedException {
        for (int i = 0; i < cyclesToTest; i++) {
            assertTrue(rateLimiter.checkLimitExceededAndIncrement("user1"));
            assertTrue(rateLimiter.checkLimitExceededAndIncrement("user1"));

            assertTrue(rateLimiter.checkLimitExceededAndIncrement("user2"));
            assertTrue(rateLimiter.checkLimitExceededAndIncrement("user2"));

            assertFalse(rateLimiter.checkLimitExceededAndIncrement("user1"));
            assertFalse(rateLimiter.checkLimitExceededAndIncrement("user2"));

            System.out.println("Wait 2 seconds in cycle %s".formatted(i + 1));
            Thread.sleep(2001);

            assertTrue(rateLimiter.checkLimitExceededAndIncrement("user1"));
            assertTrue(rateLimiter.checkLimitExceededAndIncrement("user1"));

            assertTrue(rateLimiter.checkLimitExceededAndIncrement("user2"));
            assertTrue(rateLimiter.checkLimitExceededAndIncrement("user2"));

            assertFalse(rateLimiter.checkLimitExceededAndIncrement("user1"));
            assertFalse(rateLimiter.checkLimitExceededAndIncrement("user2"));

            System.out.println("Wait 2 seconds in cycle %s".formatted(i + 1));
            Thread.sleep(2001);

            assertTrue(rateLimiter.checkLimitExceededAndIncrement("user1"));
            assertTrue(rateLimiter.checkLimitExceededAndIncrement("user2"));

            assertFalse(rateLimiter.checkLimitExceededAndIncrement("user1"));
            assertFalse(rateLimiter.checkLimitExceededAndIncrement("user2"));

            System.out.println("Wait 2 seconds in cycle %s".formatted(i + 1));
            Thread.sleep(2001);

            assertFalse(rateLimiter.checkLimitExceededAndIncrement("user1"));
            assertFalse(rateLimiter.checkLimitExceededAndIncrement("user2"));

            if (i != cyclesToTest - 1) {
                System.out.println("Wait 4 seconds in cycle %s".formatted(i + 1));
                Thread.sleep(4001);
            }
        }
        System.out.println("All tests are passed!");
    }

    private RedissonClient createClient() {
        try {
            Config config = new Config();
            SingleServerConfig singleServerConfig = config.useSingleServer();
            singleServerConfig.setAddress("redis://localhost:" + PORT);
            config.setCodec(new StringCodec());
            return Redisson.create(config);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
