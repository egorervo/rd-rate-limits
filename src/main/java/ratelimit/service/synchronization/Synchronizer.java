package ratelimit.service.synchronization;

import java.util.function.Supplier;

public interface Synchronizer {

    boolean evaluate(String mutexKey, Supplier<Boolean> action);
}
