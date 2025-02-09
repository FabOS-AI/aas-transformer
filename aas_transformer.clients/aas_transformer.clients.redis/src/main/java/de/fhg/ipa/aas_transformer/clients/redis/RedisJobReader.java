package de.fhg.ipa.aas_transformer.clients.redis;

import de.fhg.ipa.aas_transformer.model.TransformationJob;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;
import java.util.List;
import java.util.stream.Stream;

@Component
public class RedisJobReader extends RedisClient {
    private static final Logger LOG = LoggerFactory.getLogger(RedisJobReader.class);

    public RedisJobReader(RedisConnectionFactory connectionFactory) {
        super(connectionFactory);
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
