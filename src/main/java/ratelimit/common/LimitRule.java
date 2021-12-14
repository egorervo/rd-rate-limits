package ratelimit.common;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.Duration;

@Data
@AllArgsConstructor
public class LimitRule {
    private Duration duration;
    private final long limit;
}
