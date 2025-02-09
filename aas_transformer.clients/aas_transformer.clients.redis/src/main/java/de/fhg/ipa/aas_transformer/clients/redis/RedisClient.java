package de.fhg.ipa.aas_transformer.clients.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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
    private final static UUID CONSUMER_ID = UUID.randomUUID();
    private final static String REDIS_PROC_JOBS_LIST_KEY = REDIS_PROC_JOBS_LIST_KEY_PREFIX + "_" + CONSUMER_ID;

    private final ListOperations<String, RedisTransformationJob> listOps;
    private final RedisTemplate<String, RedisTransformationJob> template = new RedisTemplate<>();

    public RedisClient(RedisConnectionFactory connectionFactory) {
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
        listOps.remove(REDIS_PROC_JOBS_LIST_KEY, 1, job);
    }

    public void leftPushJob(RedisTransformationJob value) {
        listOps.leftPush(REDIS_JOBS_LIST_KEY, value);
    }

    Optional<RedisTransformationJob> moveJobInProcessingList() {
        RedisTransformationJob job = listOps.move(REDIS_JOBS_LIST_KEY, RIGHT, REDIS_PROC_JOBS_LIST_KEY, LEFT);
        return Optional.of(job);
    }

    public static UUID getConsumerId() {
        return CONSUMER_ID;
    }
}
