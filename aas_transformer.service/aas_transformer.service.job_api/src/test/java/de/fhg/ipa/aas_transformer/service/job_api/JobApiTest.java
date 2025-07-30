package de.fhg.ipa.aas_transformer.service.job_api;

import de.fhg.ipa.aas_transformer.clients.job_api.JobApiClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobProducer;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJobAction;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import reactor.core.publisher.Mono;

import java.util.UUID;

import static de.fhg.ipa.aas_transformer.test.utils.GenericTestConfig.getSimpleSubmodel;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(RedisExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class JobApiTest {
    @LocalServerPort
    private int port;

    @Autowired
    RedisJobProducer redisJobProducer;
    private JobApiClient jobApiClient;

    TransformationJob newJob = new TransformationJob(
            TransformationJobAction.EXECUTE,
            UUID.fromString("0bdd6d53-401d-4a57-a6be-af66251c8d5d"),
            "submodelId",
            null,
            "targetSubmodelId"
    );

    @PostConstruct
    public void init() {
        jobApiClient = new JobApiClient("http://localhost:" + port);
    }

    @Test
    @Order(0)
    void testContextLoaded() {
        assertNotNull(redisJobProducer);
        assertNotNull(jobApiClient);
    }

    @Test
    @Order(10)
    void testGetNextJobExpectNone() {
        TransformationJob result = jobApiClient.getNextJob().block();
        assertNull(result);
    }

    @Test
    @Order(20)
    void testCreateJobAndGetNextJobExpectJobAsResponse() throws InterruptedException {
        redisJobProducer.pushJob(newJob);
        TransformationJob result = jobApiClient.getNextJob().block();
        sleep(100);
        assertNotNull(result);
        assertEquals(newJob, result, "Expected the job returned by the API to match the one created");
        assertEquals(1, redisJobProducer.getLockCount(),"Expected the job to be removed from the waiting list");
    }

    @Test
    @Order(30)
    void testFinishJobExpectNoJobInQueueAndNoLocks() throws InterruptedException {
        jobApiClient.finishJob(newJob).block();

        assertEquals(
                0,
                redisJobProducer.getJobCountInt(),
                "Expected the job to be removed from the waiting list"
        );

        sleep(100); // Wait for the job to be removed from the processing list

        // TODO: Get Proc Job Count

        assertEquals(
                0,
                redisJobProducer.getLockCount(),
                "Expected the job to be removed from the processing list and unlocked"
        );
    }

    @Test
    @Order(40)
    void testPushJobInclSubmodelExpectNoErrors() {
        newJob.setSubmodel(getSimpleSubmodel());
        redisJobProducer.pushJob(newJob);
        TransformationJob newJobInklSubmodel = jobApiClient.getNextJob().block();
        return;
    }
}
