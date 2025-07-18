package de.fhg.ipa.aas_transformer.service.executor;

import de.fhg.ipa.aas_transformer.clients.redis.RedisJobClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisTransformationJob;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.List;

import static java.lang.Thread.sleep;

@Component
public class RedisJobConsumer extends RedisJobClient implements Runnable, ApplicationListener<ContextClosedEvent> {
    private static final Logger LOG = LoggerFactory.getLogger(RedisJobConsumer.class);
    private boolean isShuttingDown = false;

//    private Optional<RedisTransformationJob> optionalCurrentRedisJob = Optional.empty();
    private final Sinks.Many<TransformationJob> jobSink =
            Sinks.many().unicast().onBackpressureBuffer();
    private final Flux<TransformationJob> jobFlux = jobSink.asFlux();
    private Thread consumerThread = new Thread(this);

    public RedisJobConsumer(RedisConnectionFactory redisConnectionFactory) {
        super(redisConnectionFactory);
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
        redisConnectionFactory.getConnection().close();
    }

    @Override
    public void run() {
        LOG.info("RedisJobConsumer started");
        boolean isJobInProcessListEmitedToSink = false;
        while(!isShuttingDown) {
            if(this.getProcJobCountInt() == 0) {
                this.moveNextJobIntoProcessingList();
                isJobInProcessListEmitedToSink = false;
            } else {
                if(!isJobInProcessListEmitedToSink) {
                    this.getAndEmitNextJobInProcessingList();
                    isJobInProcessListEmitedToSink = true;
                } else {
                    this.sleep();
                }
            }
        }

        LOG.info("RedisJobConsumer stopped");
    }

    private void getAndEmitNextJobInProcessingList() {
        List<RedisTransformationJob> nextProcJobList = this.lookupFirstInProcJobList();
        if(nextProcJobList != null && nextProcJobList.size() > 0) {
            TransformationJob job = nextProcJobList.get(0).getTransformationJob();
            this.jobSink.tryEmitNext(job);
            LOG.info("Put job in sink for processing | sourceSmId {} | TransformerID {}",
                    job.getSubmodelId(),
                    job.getTransformerId()
            );
        }
    }

    private void sleep() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void markJobAsProcessed() throws SerializationException {
        RedisTransformationJob redisJob = this.lookupFirstInProcJobList().get(0);
        TransformationJob job = redisJob.getTransformationJob();
        LOG.info("Mark job as finished | sourceSmIdShort {} | TransformerID {}",
                job.getSubmodelId(),
                job.getTransformerId()
        );

        this.markJobAsProcessed(redisJob);
    }
}
