package de.fhg.ipa.aas_transformer.test.system.performance.listener;

import de.fhg.ipa.aas_transformer.model.ServiceType;
import de.fhg.ipa.aas_transformer.test.utils.creator.HistoricDataCreator;
import de.fhg.ipa.aas_transformer.test.utils.creator.TimeSeriesSubmodelCreator;
import de.vandermeer.asciitable.AsciiTable;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
class HistoricDataListenerTest extends AbstractListenerTest {
    /** Idea:
     * Same (saturational) load with different count of listeners.
     * => Measure performance of 1 vs. 2 vs. 3 listeners.
     * => Performance measured in average of created transformation jobs
     * => Performance measured in average of open submodel changes delta
     */

    @BeforeEach
    @AfterEach
    public void beforeAfterEach() {
        clearRedis();
    }

    static List<TestResult> testResults = new ArrayList<>();

    class TestResult {
        private final int listenerCount;
        private final int submodelCreatorCount;
        private final long createdJobCount;
        private final long testDurationInMs;
        private final long measuredTimeInMs;

        public TestResult(int listenerCount, int submodelCreatorCount, long createdJobCount, long testDurationInMs, long measuredTimeInMs) {
            this.listenerCount = listenerCount;
            this.submodelCreatorCount = submodelCreatorCount;
            this.createdJobCount = createdJobCount;
            this.testDurationInMs = testDurationInMs;
            this.measuredTimeInMs = measuredTimeInMs;
        }

        public float getAverageCreatedJobsPerSecond() {
            return createdJobCount / (measuredTimeInMs / 1000.0f);
        }

        public float getAverageCreatedJobsPerListener() {
            return createdJobCount / (float) listenerCount;
        }

        @Override
        public String toString() {
            return "TestResult{" +
                    "listenerCount=" + listenerCount +
                    ", submodelCreatorCount=" + submodelCreatorCount +
                    ", createdJobCount=" + createdJobCount +
                    ", testDurationInMs=" + testDurationInMs +
                    ", averageCreatedJobsPerSecond=" + getAverageCreatedJobsPerSecond() +
                    ", averageCreatedJobsPerListener=" + getAverageCreatedJobsPerListener() +
                    '}';
        }

        // Getters for the fields can be added here if needed
    }

    class AggregatedTestResult {
        private final List<TestResult> testResults;

        public AggregatedTestResult(List<TestResult> testResults) {
            this.testResults = testResults;
        }

        private List<TestResult> filterTestResultsByListenerCount(int listenerCount) {
            return testResults.stream()
                    .filter(r -> r.listenerCount == listenerCount)
                    .collect(Collectors.toList());
        }

        public Set<Integer> getListenerCounts() {
            return testResults.stream()
                    .map(r -> r.listenerCount)
                    .collect(Collectors.toSet());
        }

        public long getTestCount(int listenerCount) {
            return testResults.stream()
                    .filter(r -> r.listenerCount == listenerCount)
                    .count();
        }

        private Double calculateStandardDeviation(List<Float> results, Double average) {
            return Math.sqrt(
                    results.stream()
                    .mapToDouble(result -> Math.pow(result - average, 2))
                    .average()
                    .orElse(0.0)
            );
        }

        public Double getAverageCreatedJobsPerSecond(int listenerCount) {
            return filterTestResultsByListenerCount(listenerCount)
                    .stream()
                    .collect(Collectors.averagingDouble(TestResult::getAverageCreatedJobsPerSecond));
        }

        public Double getStdDevOfCreatedJobsPerSecond(int listenerCount) {
            return calculateStandardDeviation(
                    filterTestResultsByListenerCount(listenerCount).stream().map(TestResult::getAverageCreatedJobsPerSecond).toList(),
                    getAverageCreatedJobsPerSecond(listenerCount)
            );
        }

        public Double getAverageCreatedJobsPerInstance(int listenerCount) {
            return filterTestResultsByListenerCount(listenerCount)
                    .stream()
                    .collect(Collectors.averagingDouble(TestResult::getAverageCreatedJobsPerListener));
        }

        public Double getStdDevOfCreatedJobsPerInstance(int listenerCount) {
            return calculateStandardDeviation(
                    filterTestResultsByListenerCount(listenerCount).stream().map(TestResult::getAverageCreatedJobsPerListener).toList(),
                    getAverageCreatedJobsPerInstance(listenerCount)
            );
        }

        private String toString(int listenerCount) {
            return "listenerCount = " + listenerCount + ", " +
                    "Average Created Jobs per Second: " + getAverageCreatedJobsPerSecond(listenerCount) + ", " +
                    "Standard Deviation of Created Jobs per Second: " + getStdDevOfCreatedJobsPerSecond(listenerCount) + ", " +
                    "Average Created Jobs per Listener Instance: " + getAverageCreatedJobsPerInstance(listenerCount) + ", " +
                    "Standard Deviation of Created Jobs per Instance: " + getStdDevOfCreatedJobsPerInstance(listenerCount);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            for (int listenerCount : getListenerCounts()) {
                result.append(toString(listenerCount))
                        .append("\n");
            }
            return result.toString();
        }

        public String toTable() {
            AsciiTable table = new AsciiTable();
            table.addRow("", "", "Jobs/s", "", "Jobs/Instance", "");
            table.addRule();
            table.addRow("Listener Count", "Test Count", "Avg.", "StdDev", "Avg.", "StdDev");
            table.addRule();
            for (int listenerCount : getListenerCounts())
                table.addRow(
                        listenerCount,
                        getTestCount(listenerCount),
                        String.format("%.2f", getAverageCreatedJobsPerSecond(listenerCount)),
                        String.format("%.2f", getStdDevOfCreatedJobsPerSecond(listenerCount)),
                        String.format("%.2f", getAverageCreatedJobsPerInstance(listenerCount)),
                        String.format("%.2f", getStdDevOfCreatedJobsPerInstance(listenerCount))
                );
            return table.render();
        }
    }


    @Order(10)
    @ParameterizedTest
    @ValueSource(ints = {
            1, 1, 1,
            2, 2, 2,
            3, 3, 3
    })
    public void testScaleUpOfListener(int listenerCount) throws InterruptedException {
        int submodelCreatorCount = 6;
        int testDurationInMs = 30 * 1000;

        // Scale Listener:
        System.out.println("Scale Listener to "+listenerCount+" replica");
        scalingClient.enableServiceType(ServiceType.LISTENER, true).block();
        scalingClient.scaleServiceType(ServiceType.LISTENER, (long) listenerCount, false).block();
        scalingClient.enableServiceType(ServiceType.LISTENER, false).block();
        waitForListenerCount(count -> count != listenerCount);

        // Let Listeners settle:
        sleep(20*1000);

        // Create and Start HistoricDataCreator Threads:
        List<HistoricDataCreator> smCreatorThreads = new ArrayList<>();
        for(int i = 0; i < submodelCreatorCount; i++)
            smCreatorThreads.add(createHistoricDataCreator(0, 1));

        long start = System.currentTimeMillis();
        smCreatorThreads.forEach(TimeSeriesSubmodelCreator::start);

        sleep(testDurationInMs);

        // Stop Submodel Creators:
        smCreatorThreads.forEach(TimeSeriesSubmodelCreator::stop);
        redisControllerClient.deleteMessageEvents().block();
        long end = System.currentTimeMillis();

        scalingClient.enableServiceType(ServiceType.LISTENER, true).block();
        scalingClient.scaleServiceType(ServiceType.LISTENER, 0L, false).block();
        scalingClient.enableServiceType(ServiceType.LISTENER, false).block();
        Long createdJobCount = redisControllerClient.getWaitingJobCount().block();

        testResults.add(new TestResult(
                listenerCount,
                submodelCreatorCount,
                createdJobCount,
                testDurationInMs,
                end - start
                )
        );

        System.out.println("Listener Count: " + listenerCount);
        System.out.println("Submodel Creator Count: " + submodelCreatorCount);
        System.out.println("Created Jobs: " + createdJobCount);
        System.out.println("Test duration: " + testDurationInMs / 1000 + "s");
        System.out.println("Measured time: " + (end - start) / 1000 + "s");
        System.out.println("Average created jobs per second: " + (createdJobCount / ((end - start) / 1000.0)));
        System.out.println("Average created jobs per listener: " + (createdJobCount / listenerCount));
    }

    @Test
    @Order(20)
    public void testResults() {
        testResults.forEach(r -> System.out.println(r.toString()));
    }

    @Test
    @Order(30)
    public void testAggregatedResults() {
        AggregatedTestResult result = new AggregatedTestResult(testResults);
        System.out.println(result.toTable());
    }

    private void clearRedis() {
        redisControllerClient.deleteInProgressJobs().block();
        redisControllerClient.deleteWaitingJobs().block();
        redisControllerClient.deleteMessageEvents().block();
    }
}
