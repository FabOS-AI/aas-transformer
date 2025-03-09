package de.fhg.ipa.aas_transformer.test.system.performance;

import de.fhg.ipa.aas_transformer.model.Transformer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.junit.jupiter.api.*;

import java.util.function.Predicate;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesTriples;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class ExtScalingTest extends ExtAbstractPerformanceTest {

    Transformer transformer = (Transformer) getRandomTimeseriesTriples(1).get(0).get(2);

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
                true
        ).block();
    }

    @Order(10)
    @Test
    public void testAutomaticScaleUpScaleDownScaleToMinium() throws InterruptedException {
        AlertWatcher alertWatcher = new AlertWatcher(
                "http://aas-transformer.local/prometheus",
                "http://alertmanager.aas-transformer.local",
                grafanaClient
        );
        alertWatcher.start();

        System.out.println("Scale executor to 1 replica");
        scalingClient.scaleExecutorService(1L, false).block();
        assertTrue(managementClient.getAllTransformer().collectList().block().size() == 1);

        // Start Submodel Creator:
        TimeSeriesSubmodelCreator smCreatorThread = new TimeSeriesSubmodelCreator(0,15);
        smCreatorThread.start();

        // Wait for scale up:
        int expectedCount = 2;
        int runningExecutorCount = waitForExecutorCount(count -> count < expectedCount);

        assertEquals(expectedCount, runningExecutorCount);

        // Do overscaling:
        long overscaleCount = 5;
        scalingClient.scaleExecutorService(overscaleCount, true).block();

        // Wait for scale down:
        runningExecutorCount = waitForExecutorCount(count -> count == overscaleCount);
        assertEquals(overscaleCount-1, runningExecutorCount);

        // Stop Submodel Creator:
        smCreatorThread.stop();

        // Wait for Transformations are finished
        waitForWaitingJobCount(count -> count > 0);

        // Wait for the executor to scale down to the minimum:
        runningExecutorCount = waitForExecutorCount(count -> count > 1);
        assertEquals(1, runningExecutorCount);

        alertWatcher.stop();
    }

    @Order(20)
    @Test
    public void testMultipleScaleUps() throws InterruptedException {
        AlertWatcher alertWatcher = new AlertWatcher(
                "http://aas-transformer.local/prometheus",
                "http://alertmanager.aas-transformer.local",
                grafanaClient
        );
        alertWatcher.start();
        System.out.println("Scale executor to 1 replica");
        scalingClient.scaleExecutorService(1L, true).block();

        TimeSeriesSubmodelCreator smCreatorThread = new TimeSeriesSubmodelCreator(0,0);
        smCreatorThread.start();

        waitForExecutorCount(count -> count < 2);
        waitForWaitingJobCount(count -> count != 0);

        smCreatorThread.stop();
        smCreatorThread.thread.join();

        // finish remaining jobs:
        waitForWaitingJobCount(count -> count != 0);
        alertWatcher.stop();
    }

    @Order(30)
    @Test
    public void testMultipleScaleDowns() throws InterruptedException {
        AlertWatcher alertWatcher = new AlertWatcher(
                "http://aas-transformer.local/prometheus",
                "http://alertmanager.aas-transformer.local",
                grafanaClient
        );
        alertWatcher.start();
        waitForExecutorCount(count -> count > 1);

        System.out.println("Scale executor to 5 replicas");
        scalingClient.scaleExecutorService(5L, true).block();

        assertTrue(managementClient.getAllTransformer().collectList().block().size() == 1);

        TimeSeriesSubmodelCreator smCreatorThread = new TimeSeriesSubmodelCreator(0,100);
        smCreatorThread.start();

        sleep(1000);

        waitForExecutorCount(count -> count > 3);

        smCreatorThread.stop();
        smCreatorThread.thread.join();

        // finish remaining jobs:
        waitForWaitingJobCount(count -> count != 0);
        alertWatcher.stop();
    }

    private int waitForExecutorCount(Predicate<Integer> condition) throws InterruptedException {
        int runningCount = scalingClient.getExecutorCurrentRunningTasks().block();
        while(condition.test(runningCount)) {
            System.out.println("Running executor count: " + runningCount);
            sleep(1000);
            runningCount = scalingClient.getExecutorCurrentRunningTasks().block();
        }
        System.out.println("Running executor count: " + runningCount);
        return runningCount;
    }

    private long waitForWaitingJobCount(Predicate<Integer> condition) throws InterruptedException {
        long waitingJobCount = jobsClient.getWaitingJobCount().block();
        while(condition.test((int)waitingJobCount)) {
            System.out.println("Remaining open Jobs: " + waitingJobCount);
            sleep(1000);
            waitingJobCount = jobsClient.getWaitingJobCount().block();
        }
        return waitingJobCount;
    }
}
