package de.fhg.ipa.aas_transformer.clients.redis;

import de.fhg.ipa.aas_transformer.clients.redis.model.RedisJobPage;
import de.fhg.ipa.aas_transformer.clients.redis.model.RedisJobPageList;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.redis.util.RedisLockRegistry;
import org.springframework.stereotype.Component;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static java.lang.Thread.sleep;

@Component
public class RedisJobConsumer extends RedisJobClient {
    private static final Logger LOG = LoggerFactory.getLogger(RedisJobConsumer.class);
    private final static String REDIS_LOCK_REGISTRY_KEY = "executor_locks";
    private final static int LOCK_TIMEOUT = 15*1000;
    private static boolean threadIsRunning = true;

    RedisLockRegistry redisLockRegistry;

    public RedisJobConsumer(RedisConnectionFactory connectionFactory) {
        super(connectionFactory);
        redisLockRegistry = new RedisLockRegistry(redisConnectionFactory, REDIS_LOCK_REGISTRY_KEY, LOCK_TIMEOUT);
    }

    public Optional<RedisTransformationJob> moveJobInProcessingList() {
        try {
            // Find next unlocked job and lock it
            RedisTransformationJob nextJob = getNextTransformationJob();
            if (nextJob == null)
                return Optional.empty();
            Optional<RedisTransformationJob> optionalNextJob = Optional.of(nextJob);

            // Move job into processing list:
            this.listOps.remove(REDIS_JOBS_LIST_KEY, 1, optionalNextJob.get());
            this.listOps.rightPush(REDIS_PROC_JOBS_LIST_KEY_PREFIX, optionalNextJob.get());

            if (optionalNextJob.isPresent())
                LOG.info("Move job into processing list | {}", optionalNextJob.get().toStringShort());

            return optionalNextJob;
        } catch (NullPointerException e) {
            // Happens if REDIS_JOBS_LIST_KEY is empty
            return Optional.empty();
        } catch (QueryTimeoutException e) {
            LOG.warn("Running into redis query timeout because no Job available | {}", e.getMessage());
            return Optional.empty();
        }
    }

    @Nullable
    private RedisTransformationJob getNextTransformationJob() {
        int jobCount = getJobCountInt();

        for(int i = 0; i < jobCount; i++) {
            RedisTransformationJob job = listOps.index(REDIS_JOBS_LIST_KEY, i);
            if (job == null)
                continue; // Skip if job is null
            try {
                if (redisLockRegistry.obtain(job.targetSubmodelId).tryLock())
                    return job;
            } catch (IllegalArgumentException e) {
                // This means the job has no targetSubmodelId, so we skip it
                LOG.warn("Job has no target submodel id => Skip locking submodel | {}", job.toStringShort());
                return job;
            } catch (Exception e) {
                LOG.error("Error while trying to lock job {}: {}", job.toStringShort(), e.getMessage(), e);
                return job;
            }

        }
        return null;
    }

    public void markJobAsFinished(TransformationJob job) {
        RedisTransformationJob redisJob = new RedisTransformationJob(job);
        // Remove Job from processing list
        listOps.remove(REDIS_PROC_JOBS_LIST_KEY_PREFIX, 1, redisJob);

        // Release the lock for submodel based on targetSubmodelId
        try {
            redisLockRegistry.obtain(redisJob.targetSubmodelId).unlock();
//            idsToUnlock.add(redisJob.targetSubmodelId);
        } catch (IllegalArgumentException e) {
            LOG.warn("Job has no target submodel id => No lock to release | {}", redisJob.toStringShort());
        } catch (IllegalStateException e) {
            LOG.warn("Lock for job {} is already released or not held by this thread", redisJob.toStringShort());
        } catch (Exception e) {
            LOG.error("Error while releasing lock for job {}: {}", redisJob.toStringShort(), e.getMessage(), e);
        }
    }

    // JOB LOOKUPS:
    public List<TransformationJob> getWaitingJobs() {
        return super.lookupAllWaitingJobs()
                .stream()
                .map(RedisTransformationJob::getTransformationJob)
                .collect(Collectors.toList());
    }

    public List<TransformationJob> getInProgressJobs() {
        return super.lookupAllProcJobs()
                .stream()
                .map(RedisTransformationJob::getTransformationJob)
                .collect(Collectors.toList());
    }


    // JOB COUNTS:
    public int getTotalJobCount() {
        return this.getWaitingJobCount() + this.getInProgressJobCount();
    }

    public int getWaitingJobCount() {
        return super.getJobCountInt();
    }

    public int getInProgressJobCount() {
        return super.getProcJobCountInt();
    }
}
