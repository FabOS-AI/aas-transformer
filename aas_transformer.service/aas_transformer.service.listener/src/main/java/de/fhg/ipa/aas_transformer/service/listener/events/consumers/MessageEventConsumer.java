package de.fhg.ipa.aas_transformer.service.listener.events.consumers;

import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobProducer;
import de.fhg.ipa.aas_transformer.clients.redis.RedisMessageEventConsumer;
import de.fhg.ipa.aas_transformer.model.TransformationJob;
import de.fhg.ipa.aas_transformer.model.message_event.MessageEvent;
import de.fhg.ipa.aas_transformer.model.message_event.SubmodelElementMessageEvent;
import de.fhg.ipa.aas_transformer.service.listener.TransformationDetectionServiceCache;
import de.fhg.ipa.aas_transformer.model.message_event.SubmodelMessageEvent;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;

@Component
public class MessageEventConsumer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MessageEventConsumer.class);

    // Submodel Services:
    private final SubmodelRepository submodelRepository;
    private final RedisMessageEventConsumer redisMessageEventConsumer;
    private final RedisJobProducer jobProducer;
    private final TransformationDetectionServiceCache transformationDetectionServiceCache;
    private final MetricsClient metricsClient;

    public MessageEventConsumer(
            SubmodelRepository submodelRepository,
            RedisMessageEventConsumer redisMessageEventConsumer,
            RedisJobProducer jobProducer,
            TransformationDetectionServiceCache transformationDetectionServiceCache,
            MetricsClient metricsClient
    ) {
        this.submodelRepository = submodelRepository;
        this.redisMessageEventConsumer = redisMessageEventConsumer;
        this.jobProducer = jobProducer;
        this.transformationDetectionServiceCache = transformationDetectionServiceCache;
        this.metricsClient = metricsClient;
    }

    @PostConstruct
    public void init() {
        // Start processing events from the event cache:
        new Thread(this).start();
    }

    @Override
    public void run() {
        // Infinity Loop for getting Messages from Event Cache:
        while (true) {
            MessageEvent messageEvent = redisMessageEventConsumer.popMessage();
            if (messageEvent == null) continue;

            SubmodelMessageEvent submodelMessageEvent = null;

            if(messageEvent instanceof SubmodelElementMessageEvent) {
                String submodelId = ((SubmodelElementMessageEvent) messageEvent).getSubmodelId();
                try {
                    submodelMessageEvent = new SubmodelMessageEvent(
                            messageEvent.getSubmodelChangeEventType(),
                            submodelRepository.getSubmodel(submodelId)
                    );
                } catch (ElementDoesNotExistException e) {
                    LOG.info("Submodel [id='" + submodelId + "' not found --> Ignoring message");
                }
            }
            else if(messageEvent instanceof SubmodelMessageEvent) {
                submodelMessageEvent = (SubmodelMessageEvent) messageEvent;
            } else {
                LOG.warn("Received unsupported message event type: " + messageEvent.getClass().getName());
                continue;
            }

            try {
                this.processSubmodelMessageEvent(submodelMessageEvent);
            } catch (Exception e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }



    private void processSubmodelMessageEvent(SubmodelMessageEvent submodelMessageEvent) {
        List<TransformationJob> jobs = transformationDetectionServiceCache.getTransformationJobsBySubmodelMessageEvent(
                submodelMessageEvent
        );

        if(jobs.size()==0)
            LOG.info("No transformer found for submodel message event: {}", submodelMessageEvent);

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
