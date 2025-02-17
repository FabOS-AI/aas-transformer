package de.fhg.ipa.aas_transformer.service.management;

import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;


@SpringBootTest(classes = DockerHandler.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class DockerClientTest {
    @Autowired
    DockerHandler dockerHandler;

    @Test
    @Order(10)
    public void testGetExecutorCountExpectOne() {
        long executorCount = dockerHandler.getReplicaCountOfExecutorService();
        assert(executorCount == 1);
    }

    @Test
    @Order(20)
    public void testScaleUpExecutorExpectTwo() {
        dockerHandler.scaleExecutorService(2);
        assert(dockerHandler.getReplicaCountOfExecutorService() == 2);
    }

    @Test
    @Order(30)
    public void testScaleDownExecutorExpectOne() {
        dockerHandler.scaleExecutorService(1);
        assert(dockerHandler.getReplicaCountOfExecutorService() == 1);
    }

    @Test
    @Order(40)
    public void testScaleDownToZeroExpectOne() {
        dockerHandler.scaleExecutorService(0);
        assert(dockerHandler.getReplicaCountOfExecutorService() == 1);
    }
}
