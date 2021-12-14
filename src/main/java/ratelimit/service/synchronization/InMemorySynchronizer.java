package ratelimit.service.synchronization;

import com.antkorwin.xsync.XSync;

import java.util.function.Supplier;

public class InMemorySynchronizer implements Synchronizer {
    private final XSync<String> xSync = new XSync<>();

    @Override
    public boolean evaluate(String mutexKey, Supplier<Boolean> action) {
        return xSync.evaluate(mutexKey, () -> action.get());
    }
}
