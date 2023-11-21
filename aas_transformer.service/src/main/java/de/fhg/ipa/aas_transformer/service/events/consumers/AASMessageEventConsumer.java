package de.fhg.ipa.aas_transformer.service.events.consumers;

import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.TransformerServiceFactory;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.events.AASMessageEvent;
import de.fhg.ipa.aas_transformer.service.events.SubmodelMessageEvent;
import de.fhg.ipa.aas_transformer.service.events.producers.IAASMessageEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AASMessageEventConsumer extends MessageEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(AASMessageEventConsumer.class);

    private final IAASMessageEventProducer aasMessageEventProducer;

    public AASMessageEventConsumer(AASManager aasManager,
                                   AASRegistry aasRegistry,
                                   TransformerServiceFactory transformerServiceFactory,
                                   TransformerJpaRepository transformerJpaRepository,
                                   IAASMessageEventProducer aasMessageEventProducer) {
        super(aasManager, aasRegistry, transformerServiceFactory, transformerJpaRepository);
        this.aasMessageEventProducer = aasMessageEventProducer;
    }

    @Override
    public void run() {
        while (true) {
            try {
                var aasMessageEvent = (AASMessageEvent) aasMessageEventProducer.getMessageEventCache().take();
                for (var submodelDescriptor : aasMessageEvent.getAASDescriptor().getSubmodelDescriptors()) {
                    this.processSubmodelMessageEvent(new SubmodelMessageEvent(
                            aasMessageEvent.getChangeEventType(),
                            submodelDescriptor,
                            aasMessageEvent.getAASDescriptor().getIdentifier()));
                }
            } catch (Exception e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
