package de.fhg.ipa.aas_transformer.test.system.performance.listener;

import com.ethlo.time.DateTime;
import de.fhg.ipa.aas_transformer.model.ServiceType;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.system.performance.ExtAbstractPerformanceTest;
import de.vandermeer.asciitable.AsciiTable;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.junit.jupiter.api.*;

import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesTriples;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

abstract public class AbstractListenerTest extends ExtAbstractPerformanceTest {

    private static Class<?> testImplementationClass;
    Transformer transformer = (Transformer) getRandomTimeseriesTriples(1).get(0).get(2);
    static List<TestResult> testResults = new ArrayList<>();
    int submodelCreatorCount = 6;
    int testDurationInMs = 30 * 1000;
    int settleTimeInMs = 30 * 1000;

    protected AbstractListenerTest(Class<?> testImplementationClass) {
        AbstractListenerTest.testImplementationClass = testImplementationClass;
    }

    @BeforeEach
    void setUp() throws DeserializationException {
        // Clear all transformers:
        System.out.println("Clear all transformers");
        managementClient.getAllTransformer().collectList().block().forEach(
                transformer -> managementClient.deleteTransformer(transformer.getId(),false).block()
        );
        System.out.println("Clear all AAS objects");
        clearAasObjects();
        System.out.println("Create test transformer");
        managementClient.createTransformer(
                transformer,
                false
        ).block();
    }

    @BeforeEach
    @AfterEach
    public void beforeAfterEach() {
        clearRedis();
    }

    @AfterAll
    static void afterAll() {
        // Print individual results:
        testResults.forEach(r -> System.out.println(r.toString()));

        // Print aggregated results:
        AggregatedTestResult result = new AggregatedTestResult(testResults);
        String title = testImplementationClass.getSimpleName();
        System.out.println(result.toTable(title));
        result.writeTableToFile(title);
    }


    @Test
    @Order(00)
    public void testExecutorIsNotRunning() {
        int expectCount = 0;

        scalingClient.enableServiceType(ServiceType.EXECUTOR, true).block();
        scalingClient.scaleServiceType(ServiceType.EXECUTOR, (long) expectCount, false).block();
        waitForExecutorCount(c -> c != expectCount);

        assertEquals(expectCount, scalingClient.getRunningTasksOfServiceType(ServiceType.EXECUTOR).block(),
                "Executor should not have any running replicas"
        );
    }


    @Test
    @Order(01)
    public void testListenerIsRunning() {
        int expectCount = 1;
        scalingClient.enableServiceType(ServiceType.LISTENER, true).block();
        scalingClient.scaleServiceType(ServiceType.LISTENER, (long) expectCount, false).block();
        waitForListenerCount(count -> count != expectCount);

        assertNotEquals(0, scalingClient.getRunningTasksOfServiceType(ServiceType.LISTENER).block(),
                "Listener should have running replicas"
        );
    }

    protected void scaleListener(int listenerCount, boolean wait) {
        System.out.println("Scale Listener to "+listenerCount+" replica");
        scalingClient.enableServiceType(ServiceType.LISTENER, true).block();
        scalingClient.scaleServiceType(ServiceType.LISTENER, (long) listenerCount, false).block();
        System.out.println("Disable Scaling of Listener");
        scalingClient.enableServiceType(ServiceType.LISTENER, false).block();
        if(wait)
            waitForListenerCount(count -> count != listenerCount);

    }

    protected void clearRedis() {
        redisControllerClient.deleteInProgressJobs().block();
        redisControllerClient.deleteWaitingJobs().block();
        redisControllerClient.deleteMessageEvents().block();
    }

    class TestResult {
        private final int listenerCount;
        private final int submodelCreatorCount;
        private final long createdJobCount;
        private final long testDurationInMs;
        private final long measuredTimeInMs;
        private final int residualMessageEventCount;

        public TestResult(int listenerCount, int submodelCreatorCount, long createdJobCount, long testDurationInMs, long measuredTimeInMs, int residualMessageEventCount) {
            this.listenerCount = listenerCount;
            this.submodelCreatorCount = submodelCreatorCount;
            this.createdJobCount = createdJobCount;
            this.testDurationInMs = testDurationInMs;
            this.measuredTimeInMs = measuredTimeInMs;
            this.residualMessageEventCount = residualMessageEventCount;
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
                    ", residualMessageEventCount=" + residualMessageEventCount +
                    '}';
        }

        // Getters for the fields can be added here if needed
    }

    static class AggregatedTestResult {
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

        public Double getAverageResidualMessageEventCount(int listenerCount) {
            return filterTestResultsByListenerCount(listenerCount)
                    .stream()
                    .collect(Collectors.averagingDouble(r -> (double) r.residualMessageEventCount));
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

        public String toTable(String title) {
            AsciiTable table = new AsciiTable();
            table.addRule();
            table.addRow("", "", null, "Jobs/s", null, "Jobs/Instance", "Res.MsgEv.");
            table.addRule();
            table.addRow("Listener Count", "Test Count", "Avg.", "StdDev", "Avg.", "StdDev", "Avg.");
            table.addRule();
            for (int listenerCount : getListenerCounts())
                table.addRow(
                        listenerCount,
                        getTestCount(listenerCount),
                        String.format("%.2f", getAverageCreatedJobsPerSecond(listenerCount)),
                        String.format("%.2f", getStdDevOfCreatedJobsPerSecond(listenerCount)),
                        String.format("%.2f", getAverageCreatedJobsPerInstance(listenerCount)),
                        String.format("%.2f", getStdDevOfCreatedJobsPerInstance(listenerCount)),
                        String.format("%.2f", getAverageResidualMessageEventCount(listenerCount))
                );
            table.addRule();
            String testDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

            return title+"\n"+table.render()+"\n"+ testDate;
        }

        /**
         * Schreibt die Ergebnistabelle in eine Datei.
         * @param title Titel der Tabelle
         */
        public void writeTableToFile(String title) {
            String tableString = toTable(title);
            try (FileWriter writer = new FileWriter(title+".txt")) {
                writer.write(tableString);
            } catch (IOException e) {
                System.err.println("Fehler beim Schreiben der Ergebnistabelle in Datei: " + e.getMessage());
            }
        }
    }
}
