package de.fhg.ipa.aas_transformer.test.system.performance.model;

import java.time.Duration;

public class TestDuration {
    private Duration duration;
    private Duration avg;
    private Duration min;
    private Duration max;

    public TestDuration(Duration avg, Duration min, Duration max) {
        this.avg = avg;
        this.min = min;
        this.max = max;
    }

    public Long getAvgInMs() {
        return avg.toMillis();
    }

    public Long getMinInMs() {
        return min.toMillis();
    }

    public Long getMaxInMs() {
        return max.toMillis();
    }
}
