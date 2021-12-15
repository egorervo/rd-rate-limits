package ratelimit.common;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

@Data
@Builder
public class RateLimitWindowData implements Serializable {
    private long durationMilliSeconds;
    private long limit;
    private long count;
    private long windowStartTime;
}
