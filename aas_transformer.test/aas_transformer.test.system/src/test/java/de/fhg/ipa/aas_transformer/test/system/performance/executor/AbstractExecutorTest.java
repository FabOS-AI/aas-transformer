package de.fhg.ipa.aas_transformer.test.system.performance.executor;

import de.fhg.ipa.aas_transformer.model.ServiceType;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.system.performance.ExtAbstractPerformanceTest;
import de.vandermeer.asciitable.AsciiTable;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

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

abstract public class AbstractExecutorTest extends ExtAbstractPerformanceTest {

    private static Class<?> testImplementationClass;
    Transformer transformer = (Transformer) getRandomTimeseriesTriples(1).get(0).get(2);
    static List<AbstractExecutorTest.TestResult> testResults = new ArrayList<>();
    int submodelCreatorCount = 2;
    int testDurationInMs = 30 * 1000;
    int settleTimeInMs = 30 * 1000;

    protected AbstractExecutorTest(Class<?> testImplementationClass) {
        AbstractExecutorTest.testImplementationClass = testImplementationClass;
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

    protected void scaleExecutor(int executorCount, boolean wait) {
        ServiceType serviceType = ServiceType.EXECUTOR;
        System.out.println("Scale "+serviceType+" to "+executorCount+" replica");

        scalingClient.enableServiceType(serviceType, true).block();
        scalingClient.scaleServiceType(serviceType, (long) executorCount, false).block();
        System.out.println("Disable Scaling of Listener");
        scalingClient.enableServiceType(serviceType, false).block();
        if(wait)
            waitForExecutorCount(count -> count != executorCount);

    }


    @Test
    @Order(00)
    public void testListenerIsNotRunning() {
        int expectCount = 0;
        ServiceType serviceType = ServiceType.LISTENER;
        scalingClient.enableServiceType(serviceType, true).block();
        scalingClient.scaleServiceType(serviceType, (long) expectCount, false).block();
        scalingClient.enableServiceType(serviceType, false).block();
        waitForListenerCount(count -> count != expectCount);

        assertEquals(expectCount, scalingClient.getRunningTasksOfServiceType(serviceType).block(),
                serviceType+" should not have any running replicas"
        );
    }


    @Test
    @Order(01)
    public void testExecutorIsRunning() {
        int expectCount = 1;

        ServiceType serviceType = ServiceType.EXECUTOR;
        scalingClient.enableServiceType(serviceType, true).block();
        scalingClient.scaleServiceType(serviceType, (long) expectCount, false).block();
        scalingClient.enableServiceType(serviceType, false).block();
        waitForExecutorCount(c -> c != expectCount);


        assertNotEquals(0, scalingClient.getRunningTasksOfServiceType(serviceType).block(),
                serviceType+" should have running replicas"
        );
    }

    protected HistoricDataJobCreator getHistoricDataJobCreator(int submodelCount, int sleepInMs) {
        List<Transformer> transformerList = managementClient.getAllTransformer().collectList().block();
        if (transformerList.isEmpty()) {
            throw new IllegalStateException("No transformer registered");
        }
        return new HistoricDataJobCreator(
                aasRegistry,
                aasRepository,
                smRegistry,
                smRepository,
                submodelCount,
                sleepInMs,
                transformerList.get(0)
        );
    }

    protected MachineDataJobCreator getMachineDataJobCreator(int submodelCount, int sleepInMs) {
        List<Transformer> transformerList = managementClient.getAllTransformer().collectList().block();
        if (transformerList.isEmpty()) {
            throw new IllegalStateException("No transformer registered");
        }
        return new MachineDataJobCreator(
                aasRegistry,
                aasRepository,
                smRegistry,
                smRepository,
                submodelCount,
                sleepInMs,
                transformerList.get(0)
        );
    }

    class TestResult {
        private final int executorCount;
        private final int submodelCreatorCount;
        private final long transformedSubmodelCount;
        private final long testDurationInMs;
        private final long measuredTimeInMs;
        private final int residualTransformationJobCount;

        public TestResult(int executorCount, int submodelCreatorCount, long transformedSubmodelCount, long testDurationInMs, long measuredTimeInMs, int residualTransformationJobCount) {
            this.executorCount = executorCount;
            this.submodelCreatorCount = submodelCreatorCount;
            this.transformedSubmodelCount = transformedSubmodelCount;
            this.testDurationInMs = testDurationInMs;
            this.measuredTimeInMs = measuredTimeInMs;
            this.residualTransformationJobCount = residualTransformationJobCount;
        }

        public float getAverageTransformedSubmodelsPerSecond() {
            return transformedSubmodelCount / (measuredTimeInMs / 1000.0f);
        }

        public float getAverageTransformedSubmodelsPerExecutor() {
            return transformedSubmodelCount / (float) executorCount;
        }

        @Override
        public String toString() {
            return "TestResult{" +
                    "executorCount=" + executorCount +
                    ", submodelCreatorCount=" + submodelCreatorCount +
                    ", transformedSubmodelCount=" + transformedSubmodelCount +
                    ", testDurationInMs=" + testDurationInMs +
                    ", averageTransformedSubmodelsPerSecond=" + getAverageTransformedSubmodelsPerSecond() +
                    ", averageTransformedSubmodelsPerExecutor=" + getAverageTransformedSubmodelsPerExecutor() +
                    ", residualTransformationJobCount=" + residualTransformationJobCount +
                    '}';
        }

        // Getters for the fields can be added here if needed
    }

    static class AggregatedTestResult {
        private final List<TestResult> testResults;

        public AggregatedTestResult(List<TestResult> testResults) {
            this.testResults = testResults;
        }

        private List<TestResult> filterTestResultsByExecutorCount(int executorCount) {
            return testResults.stream()
                    .filter(r -> r.executorCount == executorCount)
                    .collect(Collectors.toList());
        }

        public Set<Integer> getExecutorCounts() {
            return testResults.stream()
                    .map(r -> r.executorCount)
                    .collect(Collectors.toSet());
        }

        public long getTestCount(int executorCount) {
            return testResults.stream()
                    .filter(r -> r.executorCount == executorCount)
                    .count();
        }

        public int getCreatorCount() {
            return testResults.get(0).submodelCreatorCount;
        }

        private Double calculateStandardDeviation(List<Float> results, Double average) {
            return Math.sqrt(
                    results.stream()
                            .mapToDouble(result -> Math.pow(result - average, 2))
                            .average()
                            .orElse(0.0)
            );
        }

        public Double getAverageCreatedJobsPerSecond(int executorCount) {
            return filterTestResultsByExecutorCount(executorCount)
                    .stream()
                    .collect(Collectors.averagingDouble(TestResult::getAverageTransformedSubmodelsPerSecond));
        }

        public Double getStdDevOfCreatedJobsPerSecond(int executorCount) {
            return calculateStandardDeviation(
                    filterTestResultsByExecutorCount(executorCount).stream().map(TestResult::getAverageTransformedSubmodelsPerSecond).toList(),
                    getAverageCreatedJobsPerSecond(executorCount)
            );
        }

        public Double getAverageCreatedJobsPerInstance(int executorCount) {
            return filterTestResultsByExecutorCount(executorCount)
                    .stream()
                    .collect(Collectors.averagingDouble(TestResult::getAverageTransformedSubmodelsPerExecutor));
        }

        public Double getStdDevOfCreatedJobsPerInstance(int executorCount) {
            return calculateStandardDeviation(
                    filterTestResultsByExecutorCount(executorCount).stream().map(TestResult::getAverageTransformedSubmodelsPerExecutor).toList(),
                    getAverageCreatedJobsPerInstance(executorCount)
            );
        }

        public Double getAverageResidualTransformationJobCount(int executorCount) {
            return filterTestResultsByExecutorCount(executorCount)
                    .stream()
                    .collect(Collectors.averagingDouble(r -> (double) r.residualTransformationJobCount));
        }

        private String toString(int executorCount) {
            return "executorCount = " + executorCount + ", " +
                    "Average Created Jobs per Second: " + getAverageCreatedJobsPerSecond(executorCount) + ", " +
                    "Standard Deviation of Created Jobs per Second: " + getStdDevOfCreatedJobsPerSecond(executorCount) + ", " +
                    "Average Created Jobs per Listener Instance: " + getAverageCreatedJobsPerInstance(executorCount) + ", " +
                    "Standard Deviation of Created Jobs per Instance: " + getStdDevOfCreatedJobsPerInstance(executorCount);
        }

        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            for (int executorCount : getExecutorCounts()) {
                result.append(toString(executorCount))
                        .append("\n");
            }
            return result.toString();
        }

        public String toTable(String title) {
            AsciiTable table = new AsciiTable();
            table.addRule();
            table.addRow("", "", null, "Submodels/s", null, "Submodels/Instance", "Res.Jobs");
            table.addRule();
            table.addRow("Executor Count", "Test Count", "Avg.", "StdDev", "Avg.", "StdDev", "Avg.");
            table.addRule();
            for (int executorCount : getExecutorCounts())
                table.addRow(
                        executorCount,
                        getTestCount(executorCount),
                        String.format("%.2f", getAverageCreatedJobsPerSecond(executorCount)),
                        String.format("%.2f", getStdDevOfCreatedJobsPerSecond(executorCount)),
                        String.format("%.2f", getAverageCreatedJobsPerInstance(executorCount)),
                        String.format("%.2f", getStdDevOfCreatedJobsPerInstance(executorCount)),
                        String.format("%.2f", getAverageResidualTransformationJobCount(executorCount))
                );
            table.addRule();
            String testDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String creatorInfo = "Submodel Creator Count: "+getCreatorCount();

            return title+"\n"
                    +table.render()+"\n"
                    +creatorInfo+"\n"
                    + testDate;
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
