package de.fhg.ipa.aas_transformer.test.system.performance.listener;

import de.fhg.ipa.aas_transformer.model.ServiceType;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.utils.creator.HistoricDataCreator;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
class HistoricDataListenerTest extends AbstractListenerTest {
    /** Idea:
     * Same (saturational) load with different count of listeners.
     * => Measure performance of 1 vs. 2 vs. 3 listeners.
     * => Performance measured in average of created transformation jobs
     * => Performance measured in average of open submodel changes delta
     */


    @Order(10)
    @Test
    public void testScaleUpOfListener() {
        int submodelCreatorCount = 4;

        // Scale Listener:
        System.out.println("Scale Listener to 1 replica");
        scalingClient.scaleServiceType(ServiceType.LISTENER, 1L, false).block();
        waitForListenerCount(count -> count != 1);

        // Create and Start HistoricDataCreator Threads:
        List<HistoricDataCreator> smCreatorThreads = new ArrayList<>();
        for(int i = 0; i < submodelCreatorCount; i++)
            smCreatorThreads.add(createHistoricDataCreator(0, 1));

        smCreatorThreads.forEach(t -> t.start());

        // Wait Listener to scale up:
        waitForListenerCount(count -> count == 1);

        // TODO: Measure/Compare performance of 1 vs. 2 Listener instances

        // Stop Submodel Creators:
        smCreatorThreads.forEach(t -> t.stop());
    }
}
