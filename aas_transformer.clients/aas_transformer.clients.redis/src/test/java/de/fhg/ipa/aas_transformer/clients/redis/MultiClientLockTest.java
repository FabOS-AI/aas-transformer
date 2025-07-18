package de.fhg.ipa.aas_transformer.clients.redis;


import com.redis.testcontainers.RedisContainer;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJobAction;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MultiClientLockTest {

    @Autowired
    RedisConnectionFactory redisConnectionFactory;
    @Autowired
    RedisContainer redisContainer;

    RedisJobClient redisJobClient1;
    RedisJobClient redisJobClient2;
    RedisJobClient redisJobClient3;

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
        if (redisJobClient1 == null) {
            redisJobClient1 = new RedisJobClient(redisConnectionFactory);
        }
        if (redisJobClient2 == null) {
            redisJobClient2 = new RedisJobClient(redisConnectionFactory);
        }
        if (redisJobClient3 == null) {
            redisJobClient3 = new RedisJobClient(redisConnectionFactory);
        }
    }

    @BeforeAll
    public void beforeAll() {
        // Create Jobs
        for (TransformationJob job : jobList) {
            redisJobClient1.rightPushJob(new RedisTransformationJob(job));
        }
    }

    @Test
    @Order(10)
    public void testLengthOfJobQueue() {
        assertEquals(jobList.size(), redisJobClient1.getJobCountInt(),
                "Job count in redis should match the number of jobs created");
    }

    @Test
    @Order(20)
    public void testConsecutiveCheckoutsByDifferentRedisClients() {
        job1FromJobQueue = redisJobClient1.moveNextJobIntoProcessingList().get();
        job2FromJobQueue = redisJobClient2.moveNextJobIntoProcessingList().get();

        assertEquals(targetSubmodelId1, job1FromJobQueue.getTransformationJob().getTargetSubmodelId(),
                "First job should match the first target submodel ID");
        assertEquals(targetSubmodelId2, job2FromJobQueue.getTransformationJob().getTargetSubmodelId(),
                "Second job should match the second target submodel ID");
    }

    @Test
    @Order(30)
    public void testJobCheckoutIfAvailableJobsAreLocked() {
        assertTrue(redisJobClient1.getJobCountInt() > 0,
                "There should be jobs available in redis job queue");

        Optional<RedisTransformationJob> optionalJob = redisJobClient3.moveNextJobIntoProcessingList();

        assertTrue(optionalJob.isEmpty(),"No job should be available for checkout since all jobs are locked by other clients");
    }

    @Test
    @Order(40)
    public void testFinishJobAndConsumeUnlockedJob() {
        // Finish job from client 1
        redisJobClient1.markJobAsProcessed(job1FromJobQueue);

        // Now client 3 should be able to consume the next available job
        Optional<RedisTransformationJob> optionalJob = redisJobClient3.moveJobInProcessingList();
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

        redisJobClient1.rightPushJob(new RedisTransformationJob(jobWithTargetNull));

        Optional<RedisTransformationJob> jobWithTargetNullFromQueue = redisJobClient1.moveNextJobIntoProcessingList();

        assertTrue(jobWithTargetNullFromQueue.isPresent(), "Job with null target submodel ID should be processed");

        assertDoesNotThrow(
                () -> redisJobClient1.markJobAsProcessed(jobWithTargetNullFromQueue.get()),
                "Marking job with null target submodel ID as processed should not throw an exception"
        );
    }

    @Test
    @Order(60)
    public void testCheckoutJobOnHeavyLoadedQueue() throws IOException, DeserializationException {
        Submodel tsSubmodel = getRandomTimeseriesSubmodel(10, 100);

        // Simulate a heavy loaded queue by adding more jobs
        for (int i = 0; i < 1000; i++) {
            TransformationJob job = new TransformationJob(
                    TransformationJobAction.EXECUTE,
                    UUID.randomUUID(),
                    "submodelId" + i,
                    tsSubmodel,
                    "targetSubmodelId" + i
            );
            redisJobClient1.rightPushJob(new RedisTransformationJob(job));
        }

        // Check if we can still checkout jobs
        Optional<RedisTransformationJob> optionalJob = redisJobClient1.moveNextJobIntoProcessingList();
        assertTrue(optionalJob.isPresent(), "Client 2 should be able to checkout a job from a heavy loaded queue");
    }
}
