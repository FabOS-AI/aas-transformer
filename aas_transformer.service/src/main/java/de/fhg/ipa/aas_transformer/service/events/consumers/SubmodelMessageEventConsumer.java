package de.fhg.ipa.aas_transformer.service.events.consumers;

import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.TransformerServiceFactory;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.events.SubmodelMessageEvent;
import de.fhg.ipa.aas_transformer.service.events.producers.ISubmodelMessageEventProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SubmodelMessageEventConsumer extends MessageEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelMessageEventConsumer.class);

    private final ISubmodelMessageEventProducer submodelMessageEventProducer;

    public SubmodelMessageEventConsumer(TransformerServiceFactory transformerServiceFactory,
                                        TransformerJpaRepository transformerJpaRepository,
                                        SubmodelRegistry submodelRegistry, SubmodelRepository submodelRepository,
                                        ISubmodelMessageEventProducer submodelMessageEventProducer) {
        super(transformerServiceFactory, transformerJpaRepository, submodelRegistry, submodelRepository);
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
