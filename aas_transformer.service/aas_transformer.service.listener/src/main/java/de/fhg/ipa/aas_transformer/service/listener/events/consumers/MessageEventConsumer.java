package de.fhg.ipa.aas_transformer.service.listener.events.consumers;

import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobProducer;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.service.listener.TransformationDetectionServiceCache;
import de.fhg.ipa.aas_transformer.service.listener.events.SubmodelMessageEvent;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import java.util.List;

public abstract class MessageEventConsumer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MessageEventConsumer.class);

    // Submodel Services:
    protected final SubmodelRepository submodelRepository;

    private final RedisJobProducer jobProducer;
    private final TransformationDetectionServiceCache transformationDetectionServiceCache;
    private final MetricsClient metricsClient;

    protected MessageEventConsumer(
            SubmodelRepository submodelRepository,
            RedisJobProducer jobProducer,
            TransformationDetectionServiceCache transformationDetectionServiceCache,
            MetricsClient metricsClient
    ) {
        this.submodelRepository = submodelRepository;
        this.jobProducer = jobProducer;
        this.transformationDetectionServiceCache = transformationDetectionServiceCache;
        this.metricsClient = metricsClient;
    }

    @PostConstruct
    public void init() {
        // Start processing events from the event cache:
        new Thread(this).start();
    }

    protected void processSubmodelMessageEvent(SubmodelMessageEvent submodelMessageEvent) {
        List<TransformationJob> jobs = transformationDetectionServiceCache.getTransformationJobsBySubmodelMessageEvent(
                submodelMessageEvent
        );

        jobs.forEach(job -> {
            try {
                jobProducer.pushJob(job);
            } catch (SerializationException e) {
                throw new RuntimeException(e);
            }
        });

        try {
            metricsClient.addOneToProcessedSubmodelChangeCount().block();
        } catch (Exception e) {
            LOG.error("Error while incrementing processed submodel change count: " + e.getMessage());
        }
    }

}
