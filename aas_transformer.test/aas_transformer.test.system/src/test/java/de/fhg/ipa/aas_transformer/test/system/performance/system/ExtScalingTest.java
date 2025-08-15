package de.fhg.ipa.aas_transformer.test.system.performance.system;

import de.fhg.ipa.aas_transformer.model.ServiceType;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.system.performance.watcher.AlertWatcher;
import de.fhg.ipa.aas_transformer.test.system.performance.ExtAbstractPerformanceTest;
import de.fhg.ipa.aas_transformer.test.utils.creator.HistoricDataCreator;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

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
        scalingClient.scaleServiceType(ServiceType.EXECUTOR, 1L, false).block();
        assertTrue(managementClient.getAllTransformer().collectList().block().size() == 1);

        // Start Submodel Creator:
        int submodelCreatorCount = 5;
        List<HistoricDataCreator> smCreatorThreads = new ArrayList<>();
        for(int i = 0; i < submodelCreatorCount; i++)
            smCreatorThreads.add(createHistoricDataCreator(0, 1));

        smCreatorThreads.forEach(t -> t.start());

        // Wait for scale up:
        int expectedCount = 2;
        int runningExecutorCount = waitForExecutorCount(count -> count < expectedCount);

        assertEquals(expectedCount, runningExecutorCount);

        // Do overscaling:
        long overscaleCount = 5;
        scalingClient.scaleServiceType(ServiceType.EXECUTOR, overscaleCount, true).block();

        // Wait for scale down:
        runningExecutorCount = waitForExecutorCount(count -> count >= overscaleCount);
        assertEquals(overscaleCount-1, runningExecutorCount);

        // Stop Submodel Creators:
        smCreatorThreads.forEach(t -> t.stop());

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
        scalingClient.scaleServiceType(ServiceType.EXECUTOR, 1L, true).block();

        HistoricDataCreator smCreatorThread = createHistoricDataCreator(0, 0);
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
        scalingClient.scaleServiceType(ServiceType.EXECUTOR, 5L, true).block();

        assertTrue(managementClient.getAllTransformer().collectList().block().size() == 1);

        HistoricDataCreator smCreatorThread = createHistoricDataCreator(0, 100);
        smCreatorThread.start();

        sleep(1000);

        waitForExecutorCount(count -> count > 3);

        smCreatorThread.stop();

        // finish remaining jobs:
        waitForWaitingJobCount(count -> count != 0);
        alertWatcher.stop();
    }
}
