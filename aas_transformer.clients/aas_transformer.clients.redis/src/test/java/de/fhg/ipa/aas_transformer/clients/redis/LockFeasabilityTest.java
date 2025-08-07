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
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.KeyScanOptions;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.testcontainers.utility.DockerImageName;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;

import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesSubmodel;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Disabled
public class LockFeasabilityTest {

    private final String REDIS_JOB_LIST_KEY = "jobs";
    private final String REDIS_PROC_JOB_LIST_KEY = "proc_jobs";
    private final String REDIS_LOCK_REGISTRY_KEY = "executor_locks";

    @Autowired
    RedisConnectionFactory redisConnectionFactory;
    @Autowired
    RedisContainer redisContainer;
    @Autowired
    RedisJobConsumer redisJobConsumer;

    RedisLockRegistry redisLockRegistry1;
    RedisLockRegistry redisLockRegistry2;
    ListOperations<String, RedisTransformationJob> listOps;
    RedisTemplate<String, RedisTransformationJob> template = new RedisTemplate<>();


    int jobCount = 2000; // Number of jobs to be created for testing
    int lockFirstNJobs = 10; // Number of jobs to lock for testing
    List<RedisTransformationJob> testJobs = List.of(
            new RedisTransformationJob(getJob("s1", "t1")),
            new RedisTransformationJob(getJob("s1", "t1")),
            new RedisTransformationJob(getJob("s2", "t2")),
            new RedisTransformationJob(getJob("s2", "t2")),
            new RedisTransformationJob(getJob("s3", "t3")),
            new RedisTransformationJob(getJob("s3", "t3"))
    );

    @Bean
    @ServiceConnection(name = "redis")
    static RedisContainer redisContainer() {
        return new RedisContainer(DockerImageName.parse("redis:7"));
    }

    private TransformationJob getJob(String sourceSubmodelId, String targetSubmodelId) {
        return new TransformationJob(
                UUID.randomUUID(),
                TransformationJobAction.EXECUTE,
                UUID.randomUUID(),
                sourceSubmodelId,
                getRandomTimeseriesSubmodel(5, 50),
                targetSubmodelId
        );
    }

    @BeforeAll
    void setUp() {
        redisLockRegistry1 = new RedisLockRegistry(redisConnectionFactory, REDIS_LOCK_REGISTRY_KEY, 15*1000);
        redisLockRegistry1.setRedisLockType(RedisLockRegistry.RedisLockType.SPIN_LOCK);
        redisLockRegistry2 = new RedisLockRegistry(redisConnectionFactory, REDIS_LOCK_REGISTRY_KEY, 15*1000);
        redisLockRegistry2.setRedisLockType(RedisLockRegistry.RedisLockType.SPIN_LOCK);
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
//        testJobs.forEach(job -> listOps.rightPush(REDIS_JOB_LIST_KEY, job));
        IntStream.rangeClosed(1,jobCount).forEach(i ->
                listOps.rightPush(REDIS_JOB_LIST_KEY, new RedisTransformationJob(getJob(String.valueOf(i), String.valueOf(i))))
        );

        ScanOptions so = KeyScanOptions.scanOptions(DataType.STRING)
                .match("*")
                .build();
        this.template.scan(so).stream().forEach(key -> this.template.delete(key));
    }

//region disabled tests
//    @Test
//    @Order(10)
//    @Disabled
//    public void testIterateOverAllPerformance() {
//        assertEquals(jobCount, listOps.size(REDIS_JOB_LIST_KEY));
//
//        Instant start = Instant.now();
//        Optional<RedisTransformationJob> nextJob = checkoutByIterateOverAllJobs();
//        Instant end = Instant.now();
//
//        assertTrue(nextJob.isPresent(), "Expected to find a job to process");
//
//        System.out.println("Duration: " + (end.toEpochMilli() - start.toEpochMilli()) + " ms");
//    }
//endregion

    @Test
    @Order(20)
    public void testIterateOverPages() {
        this.lockFirstNJobs(lockFirstNJobs);
        assertEquals(jobCount, listOps.size(REDIS_JOB_LIST_KEY));

        Instant start = Instant.now();
        Optional<RedisTransformationJob> nextJob = redisJobConsumer.moveJobInProcessingList();
        Instant end = Instant.now();

        assertTrue(nextJob.isPresent(), "Expected to find a job to process");

        System.out.println("Job: " + nextJob.get().toStringShort());

        System.out.println("Duration: " + (end.toEpochMilli() - start.toEpochMilli()) + " ms");
    }

    @Test
    @Order(30)
    public void testIterateOverItems() {
        this.lockFirstNJobs(lockFirstNJobs);

        assertEquals(jobCount, listOps.size(REDIS_JOB_LIST_KEY));
        assertEquals(lockFirstNJobs, getLockCount());

        Instant start = Instant.now();
        Optional<RedisTransformationJob> nextJob = this.checkoutByIterateOverJobByJob();
        Instant end = Instant.now();

        assertTrue(nextJob.isPresent(), "Expected to find a job to process");

        System.out.println("Job: " + nextJob.get().toStringShort());

        System.out.println("Duration: " + (end.toEpochMilli() - start.toEpochMilli()) + " ms");
    }

    private void lockFirstNJobs(int n) {
        // Lock the first n jobs in the job list
        List<RedisTransformationJob> jobsToLock = listOps.range(REDIS_JOB_LIST_KEY, 0, n - 1);
        if (jobsToLock != null) {
            jobsToLock.forEach(job -> {
                redisLockRegistry1.obtain(job.targetSubmodelId).lock();
//                System.out.println("Locked job: " + job.toStringShort());
            });
        }
    }

    private Optional<RedisTransformationJob> checkoutByIterateOverJobByJob() {
        int size = listOps.size(REDIS_JOB_LIST_KEY).intValue();
        System.out.println("Job list size: " + size);
        int sizeLocks = getLockCount();
        System.out.println("Lock registry size: " + sizeLocks);

        for(int i = 0; i < size; i++) {
            RedisTransformationJob job = listOps.index(REDIS_JOB_LIST_KEY, i);
            if (job == null) {
                continue; // Skip if job is null
            }

            // Check if job is locked
            if (!isJobLockedByTryLock(job)) {
                // Lock the job
                redisLockRegistry2.obtain(job.targetSubmodelId).lock();

                // Move job into processing list:
                this.listOps.remove(REDIS_JOB_LIST_KEY, 1, job);
                this.listOps.rightPush(REDIS_PROC_JOB_LIST_KEY, job);

                return Optional.of(job);
            }
        }
        sizeLocks = getLockCount();
        System.out.println("Lock registry size: " + sizeLocks);

        return Optional.empty(); // No unlocked job found
    }

    private Optional<RedisTransformationJob> checkoutByIterateOverAllJobs() {
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

    private boolean isJobLockedByTryLock(RedisTransformationJob job) {
        try {
            return !redisLockRegistry2.obtain(job.targetSubmodelId).tryLock();
        } catch (IllegalArgumentException e) {
            return false;
        }

    }

    private int getLockCount() {
        ScanOptions so = KeyScanOptions.scanOptions(DataType.STRING)
                .match("*")
                .build();
        return (int) this.template.scan(so).stream().count();
    }
}
