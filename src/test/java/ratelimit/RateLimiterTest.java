package ratelimit;

import org.junit.jupiter.api.Test;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.redisson.config.Config;
import org.redisson.config.SingleServerConfig;
import ratelimit.common.LimitRule;
import ratelimit.service.RateLimiter;
import ratelimit.service.SlidingWindowRateLimiter;
import ratelimit.service.synchronization.InMemorySynchronizer;
import ratelimit.service.synchronization.RedisSynchronizer;
import ratelimit.storage.InMemoryRateLimitStorage;
import ratelimit.storage.RedisRateLimitStorage;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RateLimiterTest {

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
            singleServerConfig.setAddress("redis://localhost:6379");
            config.setCodec(new StringCodec());
//            if (redisProperties.isAuthorized()) {
//                singleServerConfig.setPassword(redisProperties.getSecuredPassword());
//            }
            return Redisson.create(config);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


}
