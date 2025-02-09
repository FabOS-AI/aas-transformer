package de.fhg.ipa.aas_transformer.clients.redis;

import de.fhg.ipa.aas_transformer.model.TransformationJob;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

@Component
public class RedisJobProducer extends RedisClient {
    private static final Logger LOG = LoggerFactory.getLogger(RedisJobProducer.class);

    public RedisJobProducer(
            RedisConnectionFactory redisConnectionFactory
    ) {
        super(redisConnectionFactory);
    }

    public void pushJob(TransformationJob job) throws SerializationException {
        RedisTransformationJob redisJob = new RedisTransformationJob(job);
        if(isJobAlreadyInQueue(redisJob)) {
            LOG.info(
                    "Strict mode is not enabled and Job already in queue | Not pushing job to queue {}",
                    job
            );
            return;
        };

        this.leftPushJob(new RedisTransformationJob(job));
        LOG.info("Pushing transformation job to queue: {}", job);
    }

    private boolean isJobAlreadyInQueue(RedisTransformationJob job) {
        // If job contains source submodel (not only ID) always push the job to the queue (strict-mode):
        if(job.sourceSubmodel == null || job.sourceSubmodel.equals("")) {
            for(RedisTransformationJob inQueueJob : super.lookupAllWaitingJobs()) {
                if(inQueueJob.equals(job)) {
                    return true;
                }
            }
        }

        return false;
    }
}
