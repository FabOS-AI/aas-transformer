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

import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.*;
import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class MultiClientLockTest {

    @Autowired
    RedisContainer redisContainer;
    @Autowired
    RedisJobProducer redisJobProducer;
    @Autowired
    RedisJobConsumer redisJobConsumer;

    String targetSubmodelId1 = "targetSubmodelId1";
    String targetSubmodelId2 = "targetSubmodelId2";

    List<TransformationJob> jobList = List.of(
            new TransformationJob(TransformationJobAction.EXECUTE, UUID.randomUUID(), "submodelId1", null, targetSubmodelId1),
            new TransformationJob(TransformationJobAction.EXECUTE, UUID.randomUUID(), "submodelId1", null, targetSubmodelId1),
            new TransformationJob(TransformationJobAction.EXECUTE, UUID.randomUUID(), "submodelId2", null, targetSubmodelId2)
    );

    RedisTransformationJob job1FromJobQueue;
    RedisTransformationJob job2FromJobQueue;

    @BeforeAll
    public void beforeAll() {
        // Create Jobs
        for (TransformationJob job : jobList) {
            redisJobProducer.pushJob(job);
        }
    }

    @Test
    @Order(10)
    public void testLengthOfJobQueue() {
        assertEquals(jobList.size(), redisJobProducer.getJobCountInt(),
                "Job count in redis should match the number of jobs created");
    }

    @Test
    @Order(20)
    public void testCheckoutsExpectLockCreatedAndCorrectJobReturned() throws InterruptedException {
        assertTrue(redisJobProducer.getLockCount() == 0);

        job1FromJobQueue = redisJobConsumer.moveJobInProcessingList().get();
        sleep(100); // Ensure the lock is created before the next checkout

        assertTrue(redisJobConsumer.getLockCount() == 1);

        assertEquals(targetSubmodelId1, job1FromJobQueue.getTransformationJob().getTargetSubmodelId(),
                "First job should match the first target submodel ID");
    }

    @Test
    @Order(40)
    public void testFinishJobAndConsumeUnlockedJob() throws InterruptedException {
        // Get Lock Count
        int lockCount = redisJobConsumer.getLockCount();

        // Finish job from client 1
        redisJobConsumer.markJobAsFinished(job1FromJobQueue.getTransformationJob());
        sleep(200); // Ensure the lock is released before the next operation

        // Check if lock count is reduced by 1
        assertEquals(
                lockCount - 1,
                redisJobConsumer.getLockCount(),
                "Lock count should be reduced by 1 after finishing a job"
        );

        // Now client 3 should be able to consume the next available job
        Optional<RedisTransformationJob> optionalJob = redisJobConsumer.moveJobInProcessingList();
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

        redisJobProducer.pushJob(jobWithTargetNull);

        Optional<RedisTransformationJob> jobWithTargetNullFromQueue = redisJobConsumer.moveJobInProcessingList();

        assertTrue(jobWithTargetNullFromQueue.isPresent(), "Job with null target submodel ID should be processed");

        assertDoesNotThrow(
                () -> redisJobConsumer.markJobAsFinished(jobWithTargetNullFromQueue.get().getTransformationJob()),
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
            redisJobProducer.pushJob(job);
        }

        // Check if we can still checkout jobs
        Optional<RedisTransformationJob> optionalJob = redisJobConsumer.moveJobInProcessingList();
        assertTrue(optionalJob.isPresent(), "Client 1 should be able to checkout a job from a heavy loaded queue");
    }
}
