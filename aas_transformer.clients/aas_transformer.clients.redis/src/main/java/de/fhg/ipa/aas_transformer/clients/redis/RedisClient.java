package de.fhg.ipa.aas_transformer.clients.redis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.redis.util.RedisLockRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private final RedisTemplate<String, String> scriptTemplate = new RedisTemplate<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisClient(RedisConnectionFactory connectionFactory) {
        this.redisLockRegistry = new RedisLockRegistry(connectionFactory, REDIS_LOCK_REGISTRY_KEY, 15000);
        this.redisConnectionFactory = connectionFactory;
        this.listOps = template.opsForList();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer());
        template.afterPropertiesSet();

        scriptTemplate.setConnectionFactory(connectionFactory);
        scriptTemplate.setKeySerializer(new StringRedisSerializer());
        scriptTemplate.setValueSerializer(new StringRedisSerializer());
        scriptTemplate.afterPropertiesSet();
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

    protected List<RedisTransformationJob> lookupFirstInProcJobList() {
        return listOps.range(REDIS_PROC_JOBS_LIST_KEY, 0, 0);
    }

    protected Optional<RedisTransformationJob> moveNextJobIntoProcessingList() {
        try {
            Optional<RedisTransformationJob> job = this.moveJobInProcessingList();
            if (job.isPresent())
                LOG.info("Move job into processing list | {}", job.get().toStringShort());
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

    public void rightPushJob(RedisTransformationJob value) {
        listOps.rightPush(REDIS_JOBS_LIST_KEY, value);
    }

    public RedisTransformationJob getNextTransformationJob() {
        String script;
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream("get_next_job.lua")) {
            if (inputStream == null) {
                LOG.error("Lua script not found in resources");
                return null;
            }
            script = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.error("Failed to read Lua script for getting next job: {}", e.getMessage());
            return null;
        }
        RedisScript<String> redisScript = RedisScript.of(script, String.class);

        String result = scriptTemplate.execute(redisScript, List.of(), "jobs");

        if (result == null || result.isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(result, RedisTransformationJob.class);
        } catch (JsonProcessingException e) {
            LOG.error("Failed to deserialize next job from Lua script result: {}", e.getMessage());
            return null;
        }

    }

    public Optional<RedisTransformationJob> moveJobInProcessingList() {
        // Find next unlocked job and lock it
        Optional<RedisTransformationJob> optionalNextJob = Optional.of(getNextTransformationJob());

        // Move job into processing list:
        if (optionalNextJob.isPresent()) {
            // Obtain lock for the job based on its targetSubmodelId
            if(optionalNextJob.get().targetSubmodelId != null)
                redisLockRegistry.obtain(optionalNextJob.get().targetSubmodelId).lock();
            this.listOps.remove(REDIS_JOBS_LIST_KEY, 1, optionalNextJob.get());
            this.listOps.rightPush(REDIS_PROC_JOBS_LIST_KEY, optionalNextJob.get());
        }

        return optionalNextJob;
    }

    public static UUID getConsumerId() {
        return CONSUMER_ID;
    }
}
