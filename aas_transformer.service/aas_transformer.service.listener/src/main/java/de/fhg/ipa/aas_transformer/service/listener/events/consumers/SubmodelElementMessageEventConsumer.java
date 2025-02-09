package de.fhg.ipa.aas_transformer.service.listener.events.consumers;

import de.fhg.ipa.aas_transformer.clients.management.MetricsClient;
import de.fhg.ipa.aas_transformer.clients.redis.RedisJobProducer;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.listener.TransformationDetectionServiceCache;
import de.fhg.ipa.aas_transformer.service.listener.events.SubmodelElementMessageEvent;
import de.fhg.ipa.aas_transformer.service.listener.events.SubmodelMessageEvent;
import de.fhg.ipa.aas_transformer.service.listener.events.producers.ISubmodelElementMessageProducer;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SubmodelElementMessageEventConsumer extends MessageEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelElementMessageEventConsumer.class);

    private final ISubmodelElementMessageProducer submodelElementMessageProducer;

    public SubmodelElementMessageEventConsumer(
            SubmodelRepository submodelRepository,
            ISubmodelElementMessageProducer submodelElementMessageProducer,
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
        this.submodelElementMessageProducer = submodelElementMessageProducer;
    }

    @Override
    public void run() {
        // Infinity Loop for getting Messages from Event Cache:
        while (true) {
            try {
                var submodelElementMessageEvent = (SubmodelElementMessageEvent) submodelElementMessageProducer.getMessageEventCache().take();

                try {
                    this.processSubmodelMessageEvent(new SubmodelMessageEvent(
                            submodelElementMessageEvent.getChangeEventType(),
                            submodelRepository.getSubmodel(submodelElementMessageEvent.getSubmodelId())
                    ));
                } catch (ElementDoesNotExistException e) {
                    LOG.info("Submodel [id='" + submodelElementMessageEvent.getSubmodelId() + "' not found --> Ignoring message");
                }
            } catch (Exception e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
