package de.fhg.ipa.aas_transformer.clients.redis;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.clients.redis.model.RedisJobPage;
import de.fhg.ipa.aas_transformer.clients.redis.model.RedisJobPageList;
import de.fhg.ipa.aas_transformer.clients.redis.serializer.TransformationJob2RedisSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.connection.DataType;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.integration.redis.util.RedisLockRegistry;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.Lock;

public class RedisJobClient {
    private static final Logger LOG = LoggerFactory.getLogger(RedisJobClient.class);

    protected final static String REDIS_JOBS_LIST_KEY = "jobs";
    protected final static String REDIS_PROC_JOBS_LIST_KEY_PREFIX = "proc_jobs";

    protected final RedisConnectionFactory redisConnectionFactory;
    protected final ListOperations<String, RedisTransformationJob> listOps;
    private final RedisTemplate<String, RedisTransformationJob> jobTemplate = new RedisTemplate<>();
    private final RedisTemplate<String, String> stringTemplate = new RedisTemplate<>();
    private final RedisTemplate<String, String> scriptTemplate = new RedisTemplate<>();

    public RedisJobClient(RedisConnectionFactory connectionFactory) {
        this.redisConnectionFactory = connectionFactory;
        this.listOps = jobTemplate.opsForList();
        jobTemplate.setConnectionFactory(connectionFactory);
        jobTemplate.setKeySerializer(new StringRedisSerializer());
        jobTemplate.setValueSerializer(new TransformationJob2RedisSerializer());
        jobTemplate.afterPropertiesSet();

        stringTemplate.setConnectionFactory(connectionFactory);
        stringTemplate.setKeySerializer(new StringRedisSerializer());
        stringTemplate.setValueSerializer(new StringRedisSerializer());
        stringTemplate.afterPropertiesSet();

        scriptTemplate.setConnectionFactory(connectionFactory);
        scriptTemplate.setKeySerializer(new StringRedisSerializer());
        scriptTemplate.setValueSerializer(new StringRedisSerializer());
        scriptTemplate.afterPropertiesSet();
    }

    public int getJobCountInt() {
        return  listOps.size(REDIS_JOBS_LIST_KEY).intValue();
    }

    public int getProcJobCountInt() {
        return  listOps.size(REDIS_PROC_JOBS_LIST_KEY_PREFIX).intValue();
    }

    protected List<RedisTransformationJob> lookupAllWaitingJobs() {
        return listOps.range(REDIS_JOBS_LIST_KEY, 0, getJobCountInt());
    }

    protected List<RedisTransformationJob> lookupAllProcJobs() {
        return listOps.range(REDIS_PROC_JOBS_LIST_KEY_PREFIX, 0, getProcJobCountInt());
    }

    protected List<RedisTransformationJob> lookupFirstInProcJobList() {
        return listOps.range(REDIS_PROC_JOBS_LIST_KEY_PREFIX, 0, 0);
    }

    public void rightPushJob(RedisTransformationJob value) {
        listOps.rightPush(REDIS_JOBS_LIST_KEY, value);
    }


    protected boolean isJobLocked(RedisTransformationJob job) {
        ScanOptions so = KeyScanOptions.scanOptions(DataType.STRING)
                .match("*" + job.targetSubmodelId)
                .count(1)
                .build();

        return this.jobTemplate.scan(so).hasNext();
    }

    public List<String> getLocks() {
        ScanOptions so = KeyScanOptions.scanOptions(DataType.STRING)
                .match("*")
                .build();
        return this.jobTemplate.scan(so).stream().toList();
    }

    public int getLockCount() {
        return getLocks().size();
    }
}
