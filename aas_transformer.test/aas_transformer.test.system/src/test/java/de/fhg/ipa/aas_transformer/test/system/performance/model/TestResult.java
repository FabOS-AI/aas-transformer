package de.fhg.ipa.aas_transformer.test.system.performance.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TestResult {
    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public int executorCount;
    public int testCount = 1;
    public int initialSubmodelCount;
    public int finalSubmodelCount;
    private String testDurationInSecAndMs;
    public TransformationDurations transformationDurations ;
    @JsonIgnore
    public Duration testDuration;
    @JsonIgnore
    public Duration testDurationMin;
    @JsonIgnore
    public Duration testDurationMax;
    public Map<UUID, Long> executionCountMap = new HashMap<>();
    public ExecutionCountStats executionCountStats;

    public TestResult() {}

    public TestResult(
            int executorCount,
            int initialSubmodelCount,
            int finalSubmodelCount,
            Duration testDuration,
            Map<UUID, Long> executionCountMap,
            TransformationDurations transformationDurations
    ) {
        this.executorCount = executorCount;
        this.initialSubmodelCount = initialSubmodelCount;
        this.finalSubmodelCount = finalSubmodelCount;
        this.testDuration = testDuration;
        this.executionCountMap = executionCountMap;
        this.transformationDurations = transformationDurations;
        this.executionCountStats = getExecutionCountStats(executionCountMap);
    }

    public TestResult(
            int executorCount,
            int testCount,
            int initialSubmodelCount,
            int finalSubmodelCount,
            Duration testDuration,
            Duration testDurationMin,
            Duration testDurationMax,
            ExecutionCountStats executionCountStats,
            TransformationDurations transformationDurations
    ) {
        this.executorCount = executorCount;
        this.testCount = testCount;
        this.initialSubmodelCount = initialSubmodelCount;
        this.finalSubmodelCount = finalSubmodelCount;
        this.testDuration = testDuration;
        this.testDurationMin = testDurationMin;
        this.testDurationMax = testDurationMax;
        this.executionCountStats = executionCountStats;
        this.transformationDurations = transformationDurations;
    }

    private ExecutionCountStats getExecutionCountStats(Map<UUID, Long> executionCountMap) {
        double executionCountMin = executionCountMap.values().stream()
                .mapToDouble(Long::doubleValue)
                .min()
                .getAsDouble();

        double executionCountMax = executionCountMap.values().stream()
                .mapToDouble(Long::doubleValue)
                .max()
                .getAsDouble();

        double executionCountAvg = executionCountMap.values().stream()
                .mapToDouble(Long::doubleValue)
                .average()
                .getAsDouble();

        double executionCountStdDev = executionCountMap.values().stream()
                .mapToDouble(Long::doubleValue)
                .map(executionCount -> Math.pow(executionCount - executionCountAvg, 2))
                .average()
                .getAsDouble();

        return new ExecutionCountStats(executionCountMin, executionCountMax, executionCountAvg, executionCountStdDev);
    }

    public long getTestDurationInMillisAvg() {
        return testDuration.toMillis();
    }

    public long getTestDurationInMillisMin() {
        if(testDurationMin == null)
            return 0;

        return testDurationMin.toMillis();
    }

    public long getTestDurationInMillisMax() {
        if(testDurationMax == null)
            return 0;

        return testDurationMax.toMillis();
    }

    public String getTestDurationInSecAndMs() {
        long durationSeconds = testDuration.toSeconds();
        long durationMilliSeconds = testDuration.minusSeconds(durationSeconds).toMillis();
        return durationSeconds + "s " + durationMilliSeconds + "ms";
    }

    public static void toFile(List<TestResult> testResults, String filePrefix) {
        try {
            objectMapper.writeValue(
                    new File(filePrefix+"testResults.json"),
                    testResults
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "TestResult{" +
                "executorCount=" + executorCount +
                ", initialSubmodelCount=" + initialSubmodelCount +
                ", finalSubmodelCount=" + finalSubmodelCount +
                ", duration=" + testDuration.toString() +
                '}';
    }
}
