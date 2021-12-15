package ratelimit.service.synchronization;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class RedisSynchronizer implements Synchronizer {
    private final RedissonClient redissonClient;

    public RedisSynchronizer(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Override
    public boolean evaluate(String mutexKey, Supplier<Boolean> action) {
        RLock lock = redissonClient.getFairLock("lock_" + mutexKey);
        lock.lock(30, TimeUnit.SECONDS);

        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }
}
