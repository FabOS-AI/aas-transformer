package de.fhg.ipa.aas_transformer.test.system.performance.executor;

import de.fhg.ipa.aas_transformer.model.ServiceType;
import de.fhg.ipa.aas_transformer.model.Transformer;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class SimpleScalingTest extends AbstractExecutorTest {

    protected SimpleScalingTest() {
        super(SimpleScalingTest.class);
    }

    @Test
    @Order(10)
    public void testScaleUpOfExecutor() throws InterruptedException {
        System.out.println("Scale executor to 1 replica");
        scalingClient.scaleServiceType(ServiceType.EXECUTOR, 1L, false).block();
        waitForExecutorCount(c -> c != 1);

        // Start creator thread creating submodels and pushing transformation jobs
        HistoricDataJobCreator jobCreator = getHistoricDataJobCreator(0,1);

        jobCreator.start();
        waitForExecutorCount(c -> c == 1);
        jobCreator.stop();

        // Wait for Transformations are finished
        waitForWaitingJobCount(count -> count > 0);
    }

    @Test
    @Order(20)
    public void testScaleDownOfExecutor() throws InterruptedException {
        int initialCount = 2;
        System.out.println("Scale executor to "+initialCount+" replicas");
        scalingClient.scaleServiceType(ServiceType.EXECUTOR, (long) initialCount, false).block();
        waitForExecutorCount(c -> c != initialCount);

        // Start creator thread creating submodels and pushing transformation jobs
        HistoricDataJobCreator jobCreator = getHistoricDataJobCreator(0,500);

        jobCreator.start();
        waitForExecutorCount(c -> c == initialCount);
        jobCreator.stop();

        // Wait for Transformations are finished
        waitForWaitingJobCount(count -> count > 0);
    }

    @Test
    @Order(30)
    public void testScaleToMinimumOfExecutor() throws InterruptedException {
        int initialCount = 3;
        System.out.println("Scale executor to "+initialCount+" replicas");
        scalingClient.scaleServiceType(ServiceType.EXECUTOR, (long) initialCount, false).block();
        waitForExecutorCount(c -> c != initialCount);

        waitForExecutorCount(c -> c != 1);
    }
}
