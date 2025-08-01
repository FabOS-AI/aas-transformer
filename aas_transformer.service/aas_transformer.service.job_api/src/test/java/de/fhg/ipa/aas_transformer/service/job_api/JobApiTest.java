package de.fhg.ipa.aas_transformer.service.job_api;

import de.fhg.ipa.aas_transformer.clients.job_api.JobApiClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobProducer;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJobAction;
import de.fhg.ipa.aas_transformer.test.utils.extentions.RedisExtension;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.*;
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
@Disabled
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
        sleep(200);
        assertNotNull(result);
        assertEquals(newJob, result, "Expected the job returned by the API to match the one created");
        assertEquals(1, redisJobProducer.getLockCount(),"Expected the job to be locked in the processing list");
        assertEquals(1, redisJobProducer.getProcJobCountInt(),"Expected the job to be in the processing list");
    }

    @Test
    @Order(30)
    void testFinishJobExpectNoJobInQueueAndNoLocks() throws InterruptedException {
        jobApiClient.finishJob(newJob).block();

        assertEquals(0, redisJobProducer.getJobCountInt(),"Expected the job to be removed from the waiting list");

        sleep(100); // Wait for the job to be removed from the processing list

        assertEquals(0, redisJobProducer.getLockCount(),"Expected the job to be removed from the processing list and unlocked");
        assertEquals(0, redisJobProducer.getProcJobCountInt(),"Expected the job to be removed from the processing list");
    }

    @Test
    @Order(40)
    void testPushJobInclSubmodelExpectNoErrors() throws InterruptedException {
        newJob.setSubmodel(getSimpleSubmodel());
        redisJobProducer.pushJob(newJob);
        TransformationJob newJobInklSubmodel = jobApiClient.getNextJob().block();

        TransformationJob result = jobApiClient.getNextJob().block();

        // Assert Lock and Processing List Counts after getting next job
        assertEquals(1, redisJobProducer.getLockCount(),"Expected the job to be locked in the processing list");
        assertEquals(1, redisJobProducer.getProcJobCountInt(),"Expected the job to be in the processing list");

        jobApiClient.finishJob(newJobInklSubmodel).block();

        sleep(100); // Wait for the job to be removed from the processing list

        // Assert Lock and Processing List Counts after finishing the job
        assertEquals(0, redisJobProducer.getLockCount(),"Expected the job to be removed from the processing list and unlocked");
        assertEquals(0, redisJobProducer.getProcJobCountInt(),"Expected the job to be removed from the processing list");
    }

    @Test
    @Order(50)
    void testPushMultipleJobsAndConsumeMultipleJobsExpectLockAndProcCountToBeEqual() throws InterruptedException {
        TransformationJob job1 = new TransformationJob(
                TransformationJobAction.EXECUTE,
                UUID.fromString("0bdd6d53-401d-4a57-a6be-af66251c8d5d"),
                "submodelId1",
                null,
                "targetSubmodelId1"
        );
        TransformationJob job2 = new TransformationJob(
                TransformationJobAction.EXECUTE,
                UUID.fromString("0bdd6d53-401d-4a57-a6be-af66251c8d5d"),
                "submodelId2",
                null,
                "targetSubmodelId2"
        );

        redisJobProducer.pushJob(job1);
        redisJobProducer.pushJob(job2);

        assertEquals(2, redisJobProducer.getJobCountInt(),"Expected two jobs in the waiting list");

        TransformationJob result1 = jobApiClient.getNextJob().block();
        TransformationJob result2 = jobApiClient.getNextJob().block();

        assertNotNull(result1);
        assertNotNull(result2);
        assertEquals(2, redisJobProducer.getLockCount(),"Expected both jobs to be locked in the processing list");
        assertEquals(2, redisJobProducer.getProcJobCountInt(),"Expected both jobs to be in the processing list");

        jobApiClient.finishJob(result1).block();
        jobApiClient.finishJob(result2).block();

        sleep(100); // Wait for the jobs to be removed from the processing list

        assertEquals(0, redisJobProducer.getLockCount(),"Expected both jobs to be removed from the processing list and unlocked");
        assertEquals(0, redisJobProducer.getProcJobCountInt(),"Expected both jobs to be removed from the processing list");
    }
}
