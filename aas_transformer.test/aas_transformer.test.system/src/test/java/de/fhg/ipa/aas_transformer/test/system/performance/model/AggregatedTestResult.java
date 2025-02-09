package de.fhg.ipa.aas_transformer.test.system.performance.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AggregatedTestResult {
    private static ObjectMapper objectMapper = new ObjectMapper();
    static {
        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<TestResult> aggregatedTestResults = new ArrayList<>();

    public AggregatedTestResult(List<TestResult> testResults) {
        Set<Integer> uniqueExecutorCounts = testResults.stream()
                .map(testResult -> testResult.executorCount)
                .collect(Collectors.toSet());

        for(Integer executorCount : uniqueExecutorCounts) {
            List<TestResult> testResultsByExecCount = testResults.stream()
                    .filter(testResult -> testResult.executorCount == executorCount)
                    .toList();

            long testCount = testResultsByExecCount.size();

            double durationAvg = testResultsByExecCount.stream()
                    .mapToDouble(testResult -> testResult.testDuration.toMillis())
                    .average()
                    .orElse(0.0);

            double durationMin = testResultsByExecCount.stream()
                    .mapToDouble(testResult -> testResult.testDuration.toMillis())
                    .min()
                    .orElse(0.0);

            double durationMax = testResultsByExecCount.stream()
                    .mapToDouble(testResult -> testResult.testDuration.toMillis())
                    .max()
                    .orElse(0.0);

            TransformationDurations transformationDurations = getTransformationDurations(testResultsByExecCount);

            aggregatedTestResults.add(
                    new TestResult(
                            executorCount,
                            (int) testCount,
                            testResults.get(0).initialSubmodelCount,
                            testResults.get(0).finalSubmodelCount,
                            Duration.ofMillis((long) durationAvg),
                            Duration.ofMillis((long) durationMin),
                            Duration.ofMillis((long) durationMax),
                            getExecutionCountStats(testResultsByExecCount),
                            transformationDurations
                    )
            );
        }
    }

    private ExecutionCountStats getExecutionCountStats(List<TestResult> testResultsByExecCount) {
        double executionCountMin = testResultsByExecCount.stream()
                .mapToDouble(testResult -> testResult.executionCountStats.executionCountMin)
                .min()
                .orElse(0.0);

        double executionCountMax = testResultsByExecCount.stream()
                .mapToDouble(testResult -> testResult.executionCountStats.executionCountMax)
                .max()
                .orElse(0.0);

        double executionCountAvgAvg = testResultsByExecCount.stream()
                .mapToDouble(testResult -> testResult.executionCountStats.executionCountAvg)
                .average()
                .orElse(0.0);

        double executionCountStdDevAvg = testResultsByExecCount.stream()
                .mapToDouble(testResult -> testResult.executionCountStats.executionCountStdDev)
                .average()
                .orElse(0.0);

        return new ExecutionCountStats(
                executionCountMin,
                executionCountMax,
                executionCountAvgAvg,
                executionCountStdDevAvg
        );
    }

    private TransformationDurations getTransformationDurations(List<TestResult> testResultsByExecCount) {
        long transformationDurationAvg = testResultsByExecCount.stream()
                .map(testResult -> testResult.transformationDurations.transformationAvg)
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();

        long transformationDurationMin = (long) testResultsByExecCount.stream()
                .map(testResult -> testResult.transformationDurations.transformationMin)
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();

        long transformationDurationMax = (long) testResultsByExecCount.stream()
                .map(testResult -> testResult.transformationDurations.transformationMax)
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();

        long lookupSourceAvg = testResultsByExecCount.stream()
                .map(testResult -> testResult.transformationDurations.lookupSourceAvg)
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();

        long lookupSourceMin = (long) testResultsByExecCount.stream()
                .map(testResult -> testResult.transformationDurations.lookupSourceMin)
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();

        long lookupSourceMax = (long) testResultsByExecCount.stream()
                .map(testResult -> testResult.transformationDurations.lookupSourceMax)
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();

        long destinationSaveAvg = testResultsByExecCount.stream()
                .map(testResult -> testResult.transformationDurations.destinationSaveAvg)
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();

        long destinationSaveMin = (long) testResultsByExecCount.stream()
                .map(testResult -> testResult.transformationDurations.destinationSaveMin)
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();

        long destinationSaveMax = (long) testResultsByExecCount.stream()
                .map(testResult -> testResult.transformationDurations.destinationSaveMax)
                .collect(Collectors.averagingLong(Long::longValue))
                .longValue();

        return new TransformationDurations(
                transformationDurationAvg,
                transformationDurationMin,
                transformationDurationMax,
                lookupSourceAvg,
                lookupSourceMin,
                lookupSourceMax,
                destinationSaveAvg,
                destinationSaveMin,
                destinationSaveMax
        );
    }

    public void toFile(String filePrefix) {
        try {
            objectMapper.writeValue(
                    new File(filePrefix+"aggregatedTestResults.json"),
                    aggregatedTestResults
            );
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "AggregatedTestResult{" +
                "aggregatedTestResults=" + aggregatedTestResults +
                '}';
    }
}
