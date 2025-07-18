package de.fhg.ipa.aas_transformer.clients.redis;

import com.redis.testcontainers.RedisContainer;
import de.fhg.ipa.aas_transformer.clients.redis.serializer.TransformationJob2RedisSerializer;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJobAction;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
public class LockFeasabilityTest {

    private final String REDIS_JOB_LIST_KEY = "jobs";
    private final String REDIS_PROC_JOB_LIST_KEY = "proc_jobs";
    private final String REDIS_LOCK_REGISTRY_KEY = "aas_transformer_lock";

    @Autowired
    RedisConnectionFactory redisConnectionFactory;
    @Autowired
    RedisContainer redisContainer;

    RedisLockRegistry redisLockRegistry1;
    RedisLockRegistry redisLockRegistry2;
    ListOperations<String, RedisTransformationJob> listOps;
    RedisTemplate<String, RedisTransformationJob> template = new RedisTemplate<>();

    List<RedisTransformationJob> testJobs = List.of(
            new RedisTransformationJob(getJob("1")),
            new RedisTransformationJob(getJob("1")),
            new RedisTransformationJob(getJob("2")),
            new RedisTransformationJob(getJob("2")),
            new RedisTransformationJob(getJob("3")),
            new RedisTransformationJob(getJob("3"))
    );

    @Bean
    @ServiceConnection(name = "redis")
    static RedisContainer redisContainer() {
        return new RedisContainer(DockerImageName.parse("redis:7"));
    }

    private TransformationJob getJob(String submodelId) {
        return new TransformationJob(
                TransformationJobAction.EXECUTE,
                UUID.randomUUID(),
                submodelId,
                null,
                "targetSubmodelId"
        );
    }

    @BeforeAll
    void setUp() {
        redisLockRegistry1 = new RedisLockRegistry(redisConnectionFactory, REDIS_LOCK_REGISTRY_KEY);
        redisLockRegistry2 = new RedisLockRegistry(redisConnectionFactory, REDIS_LOCK_REGISTRY_KEY);
        this.listOps = template.opsForList();
        template.setConnectionFactory(redisConnectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new TransformationJob2RedisSerializer());
        template.afterPropertiesSet();
    }

    @BeforeEach
    void clearAndPopulateRedisLists() {
        // Clear both job lists before each test
        this.listOps.trim(REDIS_JOB_LIST_KEY, 1, 0);
        this.listOps.trim(REDIS_PROC_JOB_LIST_KEY, 1, 0);

        // Load Job List
        testJobs.forEach(job -> listOps.rightPush(REDIS_JOB_LIST_KEY, job));
    }

    @Test
    @Order(10)
    public void testMoveSpecificItemFromListToList() {
        List<RedisTransformationJob> jobsAtStart = this.listOps.range(REDIS_JOB_LIST_KEY, 0, -1);

        RedisTransformationJob firstJobWithIdTwo = jobsAtStart.stream()
                .filter(job -> job.sourceSubmodelId.equals("2"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No job with submodelId '2' found"));

        this.listOps.remove(REDIS_JOB_LIST_KEY, 1, firstJobWithIdTwo);
        this.listOps.rightPush(REDIS_PROC_JOB_LIST_KEY, firstJobWithIdTwo);

        List<RedisTransformationJob> jobsAtEnd = this.listOps.range(REDIS_JOB_LIST_KEY, 0, -1);
        RedisTransformationJob procJobFromRedis = this.listOps.leftPop(REDIS_PROC_JOB_LIST_KEY);


        assertEquals(jobsAtStart.size() - 1, jobsAtEnd.size(), "Job list size should decrease by 1");
        assertEquals(firstJobWithIdTwo, procJobFromRedis, "The moved job should match the one we removed from the job list");
    }

    @Test
    @Order(20)
    public void testGetNextJobBasedOnLocks() {
        // Get Job queue at start
        List<RedisTransformationJob> jobsAtStart = this.listOps.range(REDIS_JOB_LIST_KEY, 0, -1);
        List<RedisTransformationJob> procJobsAtStart = this.listOps.range(REDIS_PROC_JOB_LIST_KEY, 0, -1);

        assertEquals(0, procJobsAtStart.size(), "Proc Job list should should be empty at start");

        // Set Lock for submodel with ID "1" with Registry 1
        String idToGetLocked = jobsAtStart.get(0).sourceSubmodelId;
        redisLockRegistry1.obtain(idToGetLocked).lock();
        System.out.println("RedisPort: " +redisContainer.getMappedPort(6379));

        // Get next job with Registry 2 based on existing locks
        Optional<RedisTransformationJob> optionalNextJob = moveNextJobIntoProcessingList();

        RedisTransformationJob nextJob = null;

        if (optionalNextJob.isPresent())
            nextJob = optionalNextJob.get();

        assertNotNull(nextJob, "Next job should not be null");
        assertEquals(
                jobsAtStart.stream().filter(j -> !j.sourceSubmodelId.equals(idToGetLocked)).findFirst().get(),
                nextJob,
                "Next job should be the second job in the queue"
        );

        // Check that the job was moved to processing list:
        List<RedisTransformationJob> jobsAtProcessing = this.listOps.range(REDIS_JOB_LIST_KEY, 0, -1);
        List<RedisTransformationJob> procJobsAtProcessing = this.listOps.range(REDIS_PROC_JOB_LIST_KEY, 0, -1);

        assertEquals(jobsAtStart.size() - 1, jobsAtProcessing.size(), "Job list size should decrease by 1 after moving to processing");
        assertEquals(procJobsAtStart.size() + 1, procJobsAtProcessing.size(), "Proc Job list size should increase by 1 after moving to processing");

        // ... do Transformation with nextJob ...

        // Remove Job from processing list
        markJobAsProcessed(nextJob);

        List<RedisTransformationJob> jobsAtEnd = this.listOps.range(REDIS_JOB_LIST_KEY, 0, -1);
        List<RedisTransformationJob> procJobsAtEnd = this.listOps.range(REDIS_PROC_JOB_LIST_KEY, 0, -1);

        assertEquals(jobsAtStart.size() - 1, jobsAtEnd.size(), "Job list size should be the same as before processing");
        assertEquals(0, procJobsAtEnd.size(), "Proc Job list size should be empty after removing the job from processing");
    }

    private Optional<RedisTransformationJob> moveNextJobIntoProcessingList() {
        // Find next unlocked job and lock it
        Optional<RedisTransformationJob> optionalNextJob = this.listOps.range(REDIS_JOB_LIST_KEY, 0, -1)
                .stream()
                .filter(job -> redisLockRegistry2.obtain(job.sourceSubmodelId).tryLock())
                .findFirst();

        // Move job into processing list:
        if (optionalNextJob.isPresent()) {
            this.listOps.remove(REDIS_JOB_LIST_KEY, 1, optionalNextJob.get());
            this.listOps.rightPush(REDIS_PROC_JOB_LIST_KEY, optionalNextJob.get());
        }

        return optionalNextJob;
    }

    private void markJobAsProcessed(RedisTransformationJob job) {
        // Remove Job from processing list
        listOps.remove(REDIS_PROC_JOB_LIST_KEY, 1, job);

        // Release the lock for submodel with ID "1"
        redisLockRegistry2.obtain(job.sourceSubmodelId).unlock();
    }

}
