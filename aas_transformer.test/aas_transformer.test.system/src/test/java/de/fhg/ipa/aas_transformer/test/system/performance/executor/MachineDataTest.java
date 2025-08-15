package de.fhg.ipa.aas_transformer.test.system.performance.executor;

import de.fhg.ipa.aas_transformer.model.ServiceType;
import org.junit.jupiter.api.*;

import static java.lang.Thread.sleep;

@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MachineDataTest extends AbstractExecutorTest {
    @Test
    @Order(10)
    public void testMachineDataCreator() throws InterruptedException {
        scalingClient.scaleServiceType(ServiceType.EXECUTOR, 2L, false).block();
        waitForExecutorCount(c -> c != 2);

        MachineDataJobCreator jobCreator = getMachineDataJobCreator(0, 10);

        jobCreator.start();

        sleep(10*1000);

        jobCreator.stop();
        return;
    }
}
