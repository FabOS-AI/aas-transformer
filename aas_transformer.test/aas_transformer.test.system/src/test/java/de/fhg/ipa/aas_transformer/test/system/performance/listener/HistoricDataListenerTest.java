package de.fhg.ipa.aas_transformer.test.system.performance.listener;

import de.fhg.ipa.aas_transformer.test.utils.creator.HistoricDataCreator;
import de.fhg.ipa.aas_transformer.test.utils.creator.TimeSeriesSubmodelCreator;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static java.lang.Thread.sleep;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
class HistoricDataListenerTest extends AbstractListenerTest {

    protected HistoricDataListenerTest() {
        super(HistoricDataListenerTest.class);
    }

    @Order(10)
    @ParameterizedTest
    @ValueSource(ints = {
            1, 1, 1, 1, 1,
            2, 2, 2, 2, 2,
            3, 3, 3, 3, 3
    })
    public void doHistoricDataListenerTest(int listenerCount) throws InterruptedException {
        // Scale Listener:
        scaleListener(listenerCount, true);

        // Let Listeners settle:
        sleep(settleTimeInMs);

        // Create and Start HistoricDataCreator Threads:
        List<HistoricDataCreator> smCreatorThreads = new ArrayList<>();
        for(int i = 0; i < submodelCreatorCount; i++)
            smCreatorThreads.add(createHistoricDataCreator(0, 1));

        long start = System.currentTimeMillis();
        smCreatorThreads.forEach(TimeSeriesSubmodelCreator::start);

        sleep(testDurationInMs);

        // Stop Submodel Creators:
        smCreatorThreads.forEach(TimeSeriesSubmodelCreator::stop);
        Integer residualMessageEventCount = redisControllerClient.getMessageEventsCount().block();
        redisControllerClient.deleteMessageEvents().block();
        long end = System.currentTimeMillis();

        // Scale Listener back to 0 replicas:
        scaleListener(0, false);
        Long createdJobCount = redisControllerClient.getWaitingJobCount().block();

        testResults.add(new TestResult(
                listenerCount,
                submodelCreatorCount,
                createdJobCount,
                testDurationInMs,
                end - start,
                residualMessageEventCount
        ));

        System.out.println("Listener Count: " + listenerCount);
        System.out.println("Submodel Creator Count: " + submodelCreatorCount);
        System.out.println("Created Jobs: " + createdJobCount);
        System.out.println("Test duration: " + testDurationInMs / 1000 + "s");
        System.out.println("Measured time: " + (end - start) / 1000 + "s");
        System.out.println("Average created jobs per second: " + (createdJobCount / ((end - start) / 1000.0)));
        System.out.println("Average created jobs per listener: " + (createdJobCount / listenerCount));
        System.out.println("Residual Message Events in Redis: " + residualMessageEventCount);
    }
}
