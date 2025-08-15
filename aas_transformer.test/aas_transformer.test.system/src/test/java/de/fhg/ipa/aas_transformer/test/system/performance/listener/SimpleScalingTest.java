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
class SimpleScalingTest extends AbstractListenerTest {

    @Order(10)
    @Test
    public void testScaleUpOfListener() {
        List<Transformer> transformerList = managementClient.getAllTransformer().collectList().block();
        assertEquals(1, transformerList.size(), "There should be exactly one transformer registered");

        int submodelCreatorCount = 4;

        System.out.println("Scale Listener to 1 replica");
        scalingClient.scaleServiceType(ServiceType.LISTENER, 1L, false).block();

        waitForListenerCount(count -> count != 1);

//        List<TimeSeriesSubmodelCreator> smCreatorThreads = new ArrayList<>();
        List<HistoricDataCreator> smCreatorThreads = new ArrayList<>();
        for(int i = 0; i < submodelCreatorCount; i++)
            smCreatorThreads.add(createHistoricDataCreator(0, 1));

        smCreatorThreads.forEach(t -> t.start());

        // Wait Listener to scale up:
        waitForListenerCount(count -> count == 1);

        // Stop Submodel Creators:
        smCreatorThreads.forEach(t -> t.stop());
    }

    @Order(20)
    @Test
    public void testScaleDownOfListener() {
        int initialCount = 2;
        System.out.println("Scale Listener to "+initialCount+" replica");
        scalingClient.scaleServiceType(ServiceType.LISTENER, (long) initialCount, false).block();

        waitForListenerCount(count -> count != initialCount);

//        TimeSeriesSubmodelCreator creatorThread = createTsSubmodelCreator(0, 50);
        HistoricDataCreator creatorThread = createHistoricDataCreator(0, 50);
        creatorThread.start();

        // Wait for Listener to scale down:
        waitForListenerCount(count -> count == initialCount);

        creatorThread.stop();
    }

    @Order(30)
    @Test
    public void testScaleToMinimumOfListener() {
        System.out.println("Scale Listener to 3 replica");
        scalingClient.scaleServiceType(ServiceType.LISTENER, 3L, false).block();

        // wait for scale up
        waitForListenerCount(count -> count != 3);

        // wait for scale to minimum
        waitForListenerCount(count -> count > 1);
    }
}
