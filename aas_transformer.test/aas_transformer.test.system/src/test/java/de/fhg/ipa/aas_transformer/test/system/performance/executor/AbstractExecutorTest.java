package de.fhg.ipa.aas_transformer.test.system.performance.executor;

import de.fhg.ipa.aas_transformer.model.ServiceType;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.system.performance.ExtAbstractPerformanceTest;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesTriples;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

abstract public class AbstractExecutorTest extends ExtAbstractPerformanceTest {

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
                false
        ).block();
    }


    @Test
    @Order(00)
    public void testListenerIsNotRunning() {
        int expectCount = 0;

        scalingClient.scaleServiceType(ServiceType.LISTENER, (long) expectCount, false).block();
        waitForListenerCount(count -> count != expectCount);

        assertEquals(expectCount, scalingClient.getRunningTasksOfServiceType(ServiceType.LISTENER).block(),
                "Listener should not have any running replicas"
        );
    }


    @Test
    @Order(01)
    public void testExecutorIsRunning() {
        int expectCount = 1;

        scalingClient.scaleServiceType(ServiceType.EXECUTOR, (long) expectCount, false).block();
        waitForExecutorCount(c -> c != expectCount);


        assertNotEquals(0, scalingClient.getRunningTasksOfServiceType(ServiceType.EXECUTOR).block(),
                "Executor should have running replicas"
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
}
