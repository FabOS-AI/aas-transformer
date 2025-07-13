package de.fhg.ipa.aas_transformer.clients.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.data.redis.connection.RedisListCommands.Direction.LEFT;
import static org.springframework.data.redis.connection.RedisListCommands.Direction.RIGHT;

public class RedisClient {
    private static final Logger LOG = LoggerFactory.getLogger(RedisClient.class);

    private final static String REDIS_JOBS_LIST_KEY = "jobs";
    private final static String REDIS_PROC_JOBS_LIST_KEY_PREFIX = "proc_jobs";
    private final static String REDIS_LOCK_REGISTRY_KEY = "executor_locks";
    private final static UUID CONSUMER_ID = UUID.randomUUID();
    private final static String REDIS_PROC_JOBS_LIST_KEY = REDIS_PROC_JOBS_LIST_KEY_PREFIX + "_" + CONSUMER_ID;

    protected final RedisConnectionFactory redisConnectionFactory;
    private final RedisLockRegistry redisLockRegistry;
    private final ListOperations<String, RedisTransformationJob> listOps;
    private final RedisTemplate<String, RedisTransformationJob> template = new RedisTemplate<>();

    public RedisClient(RedisConnectionFactory connectionFactory) {
        this.redisLockRegistry = new RedisLockRegistry(connectionFactory, REDIS_LOCK_REGISTRY_KEY, 15000);
        this.redisConnectionFactory = connectionFactory;
        this.listOps = template.opsForList();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer());
        template.afterPropertiesSet();
    }

    protected int getJobCountInt() {
        return  listOps.size(REDIS_JOBS_LIST_KEY).intValue();
    }

    protected int getProcJobCountInt() {
        return  listOps.size(REDIS_PROC_JOBS_LIST_KEY).intValue();
    }

    protected List<RedisTransformationJob> lookupAllWaitingJobs() {
        return listOps.range(REDIS_JOBS_LIST_KEY, 0, getJobCountInt());
    }

    protected List<RedisTransformationJob> lookupAllProcJobs() {
        return listOps.range(REDIS_PROC_JOBS_LIST_KEY, 0, getProcJobCountInt());
    }

    protected List<RedisTransformationJob> lookupNextProcJob() {
        return listOps.range(REDIS_PROC_JOBS_LIST_KEY, 0, 0);
    }

    protected Optional<RedisTransformationJob> moveNextJobIntoProcessingList() {
        try {
            Optional<RedisTransformationJob> job = this.moveJobInProcessingList();
            if (job.isPresent())
                LOG.info("Move job into processing list | {}", job);
            return job;
        } catch (NullPointerException e) {
            // Happens if REDIS_JOBS_LIST_KEY is empty
            return Optional.empty();
        } catch (QueryTimeoutException e) {
            LOG.warn("Running into redis query timeout because no Job available | {}", e.getMessage());
            return Optional.empty();
        }
    }

    protected void markJobAsProcessed(RedisTransformationJob job) {
        // Remove Job from processing list
        listOps.remove(REDIS_PROC_JOBS_LIST_KEY, 1, job);

        // Release the lock for submodel based on targetSubmodelId
        try {
            redisLockRegistry.obtain(job.targetSubmodelId).unlock();
        } catch (IllegalArgumentException e) {
            LOG.warn("Job has no target submodel id => No lock to release | {}", job);
        } catch (IllegalStateException e) {
            LOG.warn("Lock for job {} is already released or not held by this thread", job);
        } catch (Exception e) {
            LOG.error("Error while releasing lock for job {}: {}", job, e.getMessage(), e);
        }
    }

    @Deprecated(since="Introduction of locks")
    public void leftPushJob(RedisTransformationJob value) {
        listOps.leftPush(REDIS_JOBS_LIST_KEY, value);
    }

    public void rightPushJob(RedisTransformationJob value) {
        listOps.rightPush(REDIS_JOBS_LIST_KEY, value);
    }


    public Optional<RedisTransformationJob> moveJobInProcessingList() {
        // Find next unlocked job and lock it
        Optional<RedisTransformationJob> optionalNextJob = this.listOps.range(REDIS_JOBS_LIST_KEY, 0, -1)
                .stream()
                .filter(job -> {
                    try {
                        return redisLockRegistry.obtain(job.targetSubmodelId).tryLock();
                    } catch (IllegalArgumentException e) {
                        LOG.warn("Job has no target submodel id => Start it without a lock | {}", job);
                        return true;
                    }
                })
                .findFirst();

        // Move job into processing list:
        if (optionalNextJob.isPresent()) {
            this.listOps.remove(REDIS_JOBS_LIST_KEY, 1, optionalNextJob.get());
            this.listOps.rightPush(REDIS_PROC_JOBS_LIST_KEY, optionalNextJob.get());
        }

        return optionalNextJob;

    }

    public static UUID getConsumerId() {
        return CONSUMER_ID;
    }
}
