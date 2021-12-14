package common;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RateLimitWindowData {
    private long durationMilliSeconds;
    private long limit;
    private long count;
    private long windowStartTime;
}
