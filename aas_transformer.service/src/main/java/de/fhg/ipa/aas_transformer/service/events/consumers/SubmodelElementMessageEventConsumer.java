package de.fhg.ipa.aas_transformer.service.events.consumers;

import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.TransformerServiceFactory;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.events.SubmodelElementMessageEvent;
import de.fhg.ipa.aas_transformer.service.events.SubmodelMessageEvent;
import de.fhg.ipa.aas_transformer.service.events.producers.ISubmodelElementMessageProducer;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SubmodelElementMessageEventConsumer extends MessageEventConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelElementMessageEventConsumer.class);

    private final ISubmodelElementMessageProducer submodelElementMessageProducer;

    public SubmodelElementMessageEventConsumer(TransformerServiceFactory transformerServiceFactory,
                                               TransformerJpaRepository transformerJpaRepository,
                                               SubmodelRegistry submodelRegistry, SubmodelRepository submodelRepository,
                                               ISubmodelElementMessageProducer submodelElementMessageProducer) {
        super(transformerServiceFactory, transformerJpaRepository, submodelRegistry, submodelRepository);
        this.submodelElementMessageProducer = submodelElementMessageProducer;
    }

    @Override
    public void run() {
        while (true) {
            try {
                var submodelElementMessageEvent = (SubmodelElementMessageEvent) submodelElementMessageProducer.getMessageEventCache().take();

                try {
                    var submodel = submodelRepository.getSubmodel(submodelElementMessageEvent.getSubmodelId());
                    var submodelMessageEvent = new SubmodelMessageEvent(submodelElementMessageEvent.getChangeEventType(), submodel);
                    this.processSubmodelMessageEvent(submodelMessageEvent);
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
