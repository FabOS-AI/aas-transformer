package de.fhg.ipa.aas_transformer.test.system.performance.model;

public class ExecutionCountStats {
    public double executionCountMin;
    public double executionCountMax;
    public double executionCountAvg;
    public double executionCountStdDev;

    public ExecutionCountStats(double executionCountMin, double executionCountMax, double executionCountAvg, double executionCountStdDev) {
        this.executionCountMin = executionCountMin;
        this.executionCountMax = executionCountMax;
        this.executionCountAvg = executionCountAvg;
        this.executionCountStdDev = executionCountStdDev;
    }

    public ExecutionCountStats() {
    }
}
