package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.clients.job_api.JobApiClient;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import javax.annotation.Nullable;

@Component
public class JobConsumer implements Runnable, ApplicationListener<ContextClosedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(JobConsumer.class);
    private final JobApiClient jobApiClient;
    private boolean isShuttingDown = false;

    @Nullable
    private TransformationJob optionalCurrentJob = null;
    private final Sinks.Many<TransformationJob> jobSink =
            Sinks.many().unicast().onBackpressureBuffer();
    private final Flux<TransformationJob> jobFlux = jobSink.asFlux();
    private Thread consumerThread = new Thread(this);

    public JobConsumer(JobApiClient jobApiClient) {
        this.jobApiClient = jobApiClient;
        consumerThread.start();
    }

    public Flux<TransformationJob> getJobFlux() {
        return jobFlux;
    }

    @Override
    public void onApplicationEvent(ContextClosedEvent event) {
        LOG.info("ContextClosedEvent received in RedisJobConsumer");
        isShuttingDown = true;
        try {
            consumerThread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        LOG.info("RedisJobConsumer started");
        boolean isCurrentRedisJobEmitedToSink = false;
        while(!isShuttingDown) {
            if(optionalCurrentJob == null) {
                try {
                    optionalCurrentJob = jobApiClient.getNextJob().block(); //redisJobConsumer.moveJobInProcessingList();
                    isCurrentRedisJobEmitedToSink = false;
                } catch (NullPointerException  e) {
                    this.sleep(10);
                } catch (WebClientRequestException e) {
                    LOG.error("Failed to access Job API: {}", e.getMessage());
                    this.sleep(1000);
                }
            } else {
                if(!isCurrentRedisJobEmitedToSink) {
                    this.getAndEmitNextJobInProcessingList();
                    isCurrentRedisJobEmitedToSink = true;
                } else {
                    this.sleep(10);
                }
            }
        }

        LOG.info("RedisJobConsumer stopped");
    }

    private void getAndEmitNextJobInProcessingList() {
//        List<RedisTransformationJob> nextProcJobList = this.lookupFirstInProcJobList();
//        if(nextProcJobList != null && nextProcJobList.size() > 0) {
            TransformationJob job = optionalCurrentJob;
            this.jobSink.tryEmitNext(job);
            LOG.info("Put job in sink for processing | sourceSmId {} | targetSmId {} | TransformerID {}",
                    job.getSubmodelId(),
                    job.getTargetSubmodelId(),
                    job.getTransformerId()
            );
//        }
    }

    private void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void markJobAsProcessed() throws SerializationException {
//        RedisTransformationJob redisJob = this.lookupFirstInProcJobList().get(0);
//        TransformationJob job = redisJob.getTransformationJob();
        LOG.info("Mark job as finished | sourceSmIdShort {} | TransformerID {}",
                optionalCurrentJob.getSubmodelId(),
                optionalCurrentJob.getTransformerId()
        );

        jobApiClient.finishJob(optionalCurrentJob);
//        redisJobConsumer.markJobAsFinished(job);
        this.optionalCurrentJob = null;
//        this.markJobAsProcessed(redisJob);
    }
}
