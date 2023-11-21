package de.fhg.ipa.aas_transformer.service.events.consumers;

import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.TransformerServiceFactory;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.events.ChangeEventType;
import de.fhg.ipa.aas_transformer.service.events.SubmodelElementMessageEvent;
import de.fhg.ipa.aas_transformer.service.events.SubmodelMessageEvent;
import de.fhg.ipa.aas_transformer.service.events.producers.ISubmodelElementMessageProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SubmodelElementMessageEventConsumer extends MessageEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelElementMessageEventConsumer.class);

    private final ISubmodelElementMessageProducer submodelElementMessageProducer;

    public SubmodelElementMessageEventConsumer(AASManager aasManager,
                                               AASRegistry aasRegistry,
                                               TransformerServiceFactory transformerServiceFactory,
                                               TransformerJpaRepository transformerJpaRepository,
                                               ISubmodelElementMessageProducer submodelElementMessageProducer) {
        super(aasManager, aasRegistry, transformerServiceFactory, transformerJpaRepository);
        this.submodelElementMessageProducer = submodelElementMessageProducer;
    }

    @Override
    public void run() {
        while (true) {
            try {
                var submodelElementMessageEvent = (SubmodelElementMessageEvent) submodelElementMessageProducer.getMessageEventCache().take();
                var submodelMessageEvent = new SubmodelMessageEvent(
                        submodelElementMessageEvent.getChangeEventType(),
                        submodelElementMessageEvent.getSubmodelDescriptor(),
                        submodelElementMessageEvent.getAasId());
                this.processSubmodelMessageEvent(submodelMessageEvent);
            } catch (Exception e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
