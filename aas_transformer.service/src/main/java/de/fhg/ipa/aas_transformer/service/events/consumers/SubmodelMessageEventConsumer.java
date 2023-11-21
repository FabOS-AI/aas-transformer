package de.fhg.ipa.aas_transformer.service.events.consumers;

import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.TransformerServiceFactory;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.events.SubmodelMessageEvent;
import de.fhg.ipa.aas_transformer.service.events.producers.ISubmodelMessageEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SubmodelMessageEventConsumer extends MessageEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelMessageEventConsumer.class);

    private final ISubmodelMessageEventProducer submodelMessageEventProducer;

    public SubmodelMessageEventConsumer(AASManager aasManager,
                                        AASRegistry aasRegistry,
                                        TransformerServiceFactory transformerServiceFactory,
                                        TransformerJpaRepository transformerJpaRepository,
                                        ISubmodelMessageEventProducer submodelMessageEventProducer) {
        super(aasManager, aasRegistry, transformerServiceFactory, transformerJpaRepository);
        this.submodelMessageEventProducer = submodelMessageEventProducer;
    }

    @Override
    public void run() {
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
