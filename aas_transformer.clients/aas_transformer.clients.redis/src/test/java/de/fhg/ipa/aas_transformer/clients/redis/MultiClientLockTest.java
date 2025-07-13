package de.fhg.ipa.aas_transformer.clients.redis;


import com.redis.testcontainers.RedisContainer;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJobAction;
import jakarta.annotation.PostConstruct;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MultiClientLockTest {

    @Autowired
    RedisConnectionFactory redisConnectionFactory;
    @Autowired
    RedisContainer redisContainer;

    RedisClient redisClient1;
    RedisClient redisClient2;
    RedisClient redisClient3;

    String targetSubmodelId1 = "targetSubmodelId1";
    String targetSubmodelId2 = "targetSubmodelId2";

    List<TransformationJob> jobList = List.of(
            new TransformationJob(TransformationJobAction.EXECUTE, UUID.randomUUID(), "submodelId1", null, targetSubmodelId1),
            new TransformationJob(TransformationJobAction.EXECUTE, UUID.randomUUID(), "submodelId1", null, targetSubmodelId1),
            new TransformationJob(TransformationJobAction.EXECUTE, UUID.randomUUID(), "submodelId2", null, targetSubmodelId2)
    );

    RedisTransformationJob job1FromJobQueue;
    RedisTransformationJob job2FromJobQueue;

    @PostConstruct
    public void init() {
        if (redisClient1 == null) {
            redisClient1 = new RedisClient(redisConnectionFactory);
        }
        if (redisClient2 == null) {
            redisClient2 = new RedisClient(redisConnectionFactory);
        }
        if (redisClient3 == null) {
            redisClient3 = new RedisClient(redisConnectionFactory);
        }
    }

    @BeforeAll
    public void beforeAll() {
        // Create Jobs
        for (TransformationJob job : jobList) {
            redisClient1.rightPushJob(new RedisTransformationJob(job));
        }
    }

    @Test
    @Order(10)
    public void testLengthOfJobQueue() {
        assertEquals(jobList.size(), redisClient1.getJobCountInt(),
                "Job count in redis should match the number of jobs created");
    }

    @Test
    @Order(20)
    public void testConsecutiveCheckoutsByDifferentRedisClients() {
        job1FromJobQueue = redisClient1.moveJobInProcessingList().get();
        job2FromJobQueue = redisClient2.moveJobInProcessingList().get();

        assertEquals(targetSubmodelId1, job1FromJobQueue.getTransformationJob().getTargetSubmodelId(),
                "First job should match the first target submodel ID");
        assertEquals(targetSubmodelId2, job2FromJobQueue.getTransformationJob().getTargetSubmodelId(),
                "Second job should match the second target submodel ID");
    }

    @Test
    @Order(30)
    public void testJobCheckoutIfAvailableJobsAreLocked() {
        assertTrue(redisClient1.getJobCountInt() > 0,
                "There should be jobs available in redis job queue");

        Optional<RedisTransformationJob> optionalJob = redisClient3.moveJobInProcessingList();

        assertTrue(optionalJob.isEmpty(),"No job should be available for checkout since all jobs are locked by other clients");
    }

    @Test
    @Order(40)
    public void testFinsishJobAndConsumeUnlockedJob() {
        // Finish job from client 1
        redisClient1.markJobAsProcessed(job1FromJobQueue);

        // Now client 3 should be able to consume the next available job
        Optional<RedisTransformationJob> optionalJob = redisClient3.moveJobInProcessingList();
        assertTrue(optionalJob.isPresent(), "Client 3 should be able to consume an unlocked job");
    }

    @Test
    @Order(50)
    public void testTargetSubmodelIdOfJobBeingNull() {
        TransformationJob jobWithTargetNull = new TransformationJob(
                TransformationJobAction.EXECUTE,
                UUID.randomUUID(),
                "submodelId1",
                null,
                null
        );

        redisClient1.rightPushJob(new RedisTransformationJob(jobWithTargetNull));

        Optional<RedisTransformationJob> jobWithTargetNullFromQueue = redisClient1.moveJobInProcessingList();

        assertTrue(jobWithTargetNullFromQueue.isPresent(), "Job with null target submodel ID should be processed");

        assertDoesNotThrow(
                () -> redisClient1.markJobAsProcessed(jobWithTargetNullFromQueue.get()),
                "Marking job with null target submodel ID as processed should not throw an exception"
        );
        ;
    }
}
