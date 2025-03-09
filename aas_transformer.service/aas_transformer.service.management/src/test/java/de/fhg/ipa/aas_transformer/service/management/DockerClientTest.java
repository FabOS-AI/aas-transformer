package de.fhg.ipa.aas_transformer.service.management;

import com.github.dockerjava.api.model.Service;
import de.fhg.ipa.aas_transformer.service.management.exceptions.WaitForScaleTimeoutException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;


@SpringBootTest(classes = TransformerServiceHandler.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class DockerClientTest {
    @Autowired
    TransformerServiceHandler transformerServiceHandler;

    @Test
    @Order(10)
    public void testGetExecutorCountExpectOne() {
        long executorCount = transformerServiceHandler.getReplicaCountOfExecutorService();
        assert(executorCount == 1);
    }

    @Test
    @Order(20)
    public void testScaleUpExecutorExpectTwo() throws WaitForScaleTimeoutException {
        transformerServiceHandler.scaleExecutorService(2, false);
        assert(transformerServiceHandler.getReplicaCountOfExecutorService() == 2);
    }
    
    @Test
    @Order(30)
    public void testWaitForExecutorScaleUpFinished() {
        assertDoesNotThrow(() -> {
            transformerServiceHandler.waitExecutorScaleToFinish();
        });
    }

    @Test
    @Order(40)
    public void testScaleUpExecutorAndWaitExpectNoException() throws WaitForScaleTimeoutException {
        assertDoesNotThrow(() -> {
            transformerServiceHandler.scaleExecutorService(3, true);
        });
        assert(transformerServiceHandler.getReplicaCountOfExecutorService() == 3);
    }

    @Test
    @Order(50)
    public void testScaleDownExecutorExpectOne() throws WaitForScaleTimeoutException {
        transformerServiceHandler.scaleExecutorService(1, false);
        assert(transformerServiceHandler.getReplicaCountOfExecutorService() == 1);
    }

    @Test
    @Order(60)
    public void testScaleDownToZeroExpectOne() throws WaitForScaleTimeoutException {
        transformerServiceHandler.scaleExecutorService(0, false);
        assert(transformerServiceHandler.getReplicaCountOfExecutorService() == 1);
    }
}
