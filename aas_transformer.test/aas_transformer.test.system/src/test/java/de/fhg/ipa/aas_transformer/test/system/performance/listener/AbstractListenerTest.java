package de.fhg.ipa.aas_transformer.test.system.performance.listener;

import de.fhg.ipa.aas_transformer.model.ServiceType;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.system.performance.ExtAbstractPerformanceTest;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesTriples;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

abstract public class AbstractListenerTest extends ExtAbstractPerformanceTest {

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
    public void testExecutorIsNotRunning() {
        int expectCount = 0;

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

        scalingClient.scaleServiceType(ServiceType.LISTENER, (long) expectCount, false).block();
        waitForListenerCount(count -> count != expectCount);

        assertNotEquals(0, scalingClient.getRunningTasksOfServiceType(ServiceType.LISTENER).block(),
                "Listener should have running replicas"
        );
    }
}
