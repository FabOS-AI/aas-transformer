package de.fhg.ipa.aas_transformer.service.listener.events.consumers;

import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobProducer;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.listener.TransformationDetectionServiceCache;
import de.fhg.ipa.aas_transformer.service.listener.events.SubmodelMessageEvent;
import de.fhg.ipa.aas_transformer.service.listener.events.producers.ISubmodelMessageEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SubmodelMessageEventConsumer extends MessageEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelMessageEventConsumer.class);

    private final ISubmodelMessageEventProducer submodelMessageEventProducer;

    public SubmodelMessageEventConsumer(
            SubmodelRepository submodelRepository,
            ISubmodelMessageEventProducer submodelMessageEventProducer,
            RedisJobProducer redisJobProducer,
            TransformationDetectionServiceCache transformationDetectionServiceCache,
            MetricsClient metricsClient
    ) {
        super(
                submodelRepository,
                redisJobProducer,
                transformationDetectionServiceCache,
                metricsClient
        );
        this.submodelMessageEventProducer = submodelMessageEventProducer;
    }

    @Override
    public void run() {
        // Infinity Loop for getting Messages from Event Cache:
        while (true) {
            try {
                var submodelMessageEvent = (SubmodelMessageEvent)submodelMessageEventProducer.getMessageEventCache().take();
                this.processSubmodelMessageEvent(submodelMessageEvent);
            } catch (Exception e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
