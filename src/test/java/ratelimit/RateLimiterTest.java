package ratelimit;

import common.LimitRule;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class RateLimiterTest {

    @Test
    public void rateLimitsTest() throws InterruptedException {
        RateLimiter rateLimiter = new SlidingWindowRateLimiter(Set.of(
                new LimitRule(Duration.ofSeconds(2), 2),
                new LimitRule(Duration.ofSeconds(10), 5)
        ));

        int cyclesToTest = 3;
        for (int i = 0; i < cyclesToTest; i++) {
            assertTrue(rateLimiter.isLimitNotExceeded("user1"));
            assertTrue(rateLimiter.isLimitNotExceeded("user1"));

            assertTrue(rateLimiter.isLimitNotExceeded("user2"));
            assertTrue(rateLimiter.isLimitNotExceeded("user2"));

            assertFalse(rateLimiter.isLimitNotExceeded("user1"));
            assertFalse(rateLimiter.isLimitNotExceeded("user2"));

            System.out.println("Wait 2 seconds in cycle %s".formatted(i + 1));
            Thread.sleep(2001);

            assertTrue(rateLimiter.isLimitNotExceeded("user1"));
            assertTrue(rateLimiter.isLimitNotExceeded("user1"));

            assertTrue(rateLimiter.isLimitNotExceeded("user2"));
            assertTrue(rateLimiter.isLimitNotExceeded("user2"));

            assertFalse(rateLimiter.isLimitNotExceeded("user1"));
            assertFalse(rateLimiter.isLimitNotExceeded("user2"));

            System.out.println("Wait 2 seconds in cycle %s".formatted(i + 1));
            Thread.sleep(2001);

            assertTrue(rateLimiter.isLimitNotExceeded("user1"));
            assertTrue(rateLimiter.isLimitNotExceeded("user2"));

            assertFalse(rateLimiter.isLimitNotExceeded("user1"));
            assertFalse(rateLimiter.isLimitNotExceeded("user2"));

            System.out.println("Wait 2 seconds in cycle %s".formatted(i + 1));
            Thread.sleep(2001);

            assertFalse(rateLimiter.isLimitNotExceeded("user1"));
            assertFalse(rateLimiter.isLimitNotExceeded("user2"));

            if (i != cyclesToTest - 1) {
                System.out.println("Wait 4 seconds in cycle %s".formatted(i + 1));
                Thread.sleep(4001);
            }
        }
        System.out.println("All tests are passed!");
    }

}
