package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.model.TransformationJob;
import jakarta.annotation.PostConstruct;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class JobSinkListener {
    private static final Logger LOG = LoggerFactory.getLogger(JobSinkListener.class);
    private final RedisJobConsumer redisJobConsumer;
    private final Executor executor;

    public JobSinkListener(RedisJobConsumer redisJobConsumer, Executor executor) {
        this.redisJobConsumer = redisJobConsumer;
        this.executor = executor;
    }

    @PostConstruct
    public void postConstruct() {
        redisJobConsumer.getJobFlux().subscribe(this::handleNextJob);
    }

    private void handleNextJob(TransformationJob job) {
        // Process the job:
        executor.execute(job);

        // Mark the job as processed:
        try {
            redisJobConsumer.markJobAsProcessed();
        } catch (SerializationException e) {
            LOG.error("Failed to mark job as processed", e.getMessage());
        }
    }
}

//    @Override
//    public void run() {
//        LOG.info("JobQueueListener started");
//        boolean isRunning = true;
//        while(isRunning) {
//            if(redisJobConsumer.getCurrentJob().isEmpty())
//                continue;
//            // Process the job:
//            executor.execute(
//                    redisJobConsumer.getCurrentJob().get()
//            );
//
//            // Mark the job as processed:
//            try {
//                redisJobConsumer.markJobAsProcessed();
//            } catch (SerializationException e) {
//                LOG.error("Failed to mark job as processed", e.getMessage());
//            }
//        }
//        LOG.info("JobQueueListener stopped");
//    }
//}
