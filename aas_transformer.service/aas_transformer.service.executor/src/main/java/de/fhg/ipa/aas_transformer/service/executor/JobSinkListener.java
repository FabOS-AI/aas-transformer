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
    private final JobConsumer jobConsumer;
    private final Executor executor;

    public JobSinkListener(JobConsumer jobConsumer, Executor executor) {
        this.jobConsumer = jobConsumer;
        this.executor = executor;
    }

    @PostConstruct
    public void postConstruct() {
        jobConsumer
                .getJobFlux()
                .subscribe(this::handleNextJob);
    }

    private void handleNextJob(TransformationJob job) {
        // Process the job:
        executor.execute(job);

        // Mark the job as processed:
        try {
            jobConsumer.markJobAsProcessed();
        } catch (SerializationException e) {
            LOG.error("Failed to mark job as processed", e.getMessage());
        }
    }
}
