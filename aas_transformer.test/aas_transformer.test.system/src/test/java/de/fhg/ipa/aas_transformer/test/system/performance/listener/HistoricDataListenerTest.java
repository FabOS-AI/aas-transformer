package de.fhg.ipa.aas_transformer.test.system.performance.listener;

import de.fhg.ipa.aas_transformer.clients.management.RedisControllerClient;
import de.fhg.ipa.aas_transformer.model.ServiceType;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.test.utils.creator.HistoricDataCreator;
import de.fhg.ipa.aas_transformer.test.utils.creator.TimeSeriesSubmodelCreator;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;
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
    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, })
    public void testScaleUpOfListener(int listenerCount) throws InterruptedException {
        int submodelCreatorCount = 4;
        int testDurationInMs = 30 * 1000;

        // Scale Listener:
        System.out.println("Scale Listener to "+listenerCount+" replica");
        scalingClient.enableServiceType(ServiceType.LISTENER, true).block();
        scalingClient.scaleServiceType(ServiceType.LISTENER, (long) listenerCount, false).block();
        scalingClient.enableServiceType(ServiceType.LISTENER, false).block();
        waitForListenerCount(count -> count != listenerCount);

        // Create and Start HistoricDataCreator Threads:
        List<HistoricDataCreator> smCreatorThreads = new ArrayList<>();
        for(int i = 0; i < submodelCreatorCount; i++)
            smCreatorThreads.add(createHistoricDataCreator(0, 1));

        smCreatorThreads.forEach(TimeSeriesSubmodelCreator::start);

        sleep(testDurationInMs);

        // Stop Submodel Creators:
        smCreatorThreads.forEach(TimeSeriesSubmodelCreator::stop);
        redisControllerClient.deleteMessageEvents().block();

        scalingClient.enableServiceType(ServiceType.LISTENER, true).block();
        scalingClient.scaleServiceType(ServiceType.LISTENER, 0L, false).block();
        scalingClient.enableServiceType(ServiceType.LISTENER, false).block();
        Long createdJobCount = redisControllerClient.getWaitingJobCount().block();

        System.out.println("Listener Count: " + listenerCount);
        System.out.println("Submodel Creator Count: " + submodelCreatorCount);
        System.out.println("Created Jobs: " + createdJobCount);
        System.out.println("Test duration: " + testDurationInMs / 1000 + "s");
        clearRedis();
    }

    private void clearRedis() {
        redisControllerClient.deleteInProgressJobs().block();
        redisControllerClient.deleteWaitingJobs().block();
        redisControllerClient.deleteMessageEvents().block();
    }
}
