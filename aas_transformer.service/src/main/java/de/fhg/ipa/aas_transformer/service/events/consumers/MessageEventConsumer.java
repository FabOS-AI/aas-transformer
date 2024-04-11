package de.fhg.ipa.aas_transformer.service.events.consumers;

import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.TransformerServiceFactory;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.events.SubmodelMessageEvent;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

public abstract class MessageEventConsumer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MessageEventConsumer.class);

    protected final TransformerServiceFactory transformerServiceFactory;

    protected final TransformerJpaRepository transformerJpaRepository;

    protected final SubmodelRegistry submodelRegistry;

    protected final SubmodelRepository submodelRepository;

    protected MessageEventConsumer(TransformerServiceFactory transformerServiceFactory,
                                   TransformerJpaRepository transformerJpaRepository,
                                   SubmodelRegistry submodelRegistry, SubmodelRepository submodelRepository) {
        this.transformerServiceFactory = transformerServiceFactory;
        this.transformerJpaRepository = transformerJpaRepository;
        this.submodelRegistry = submodelRegistry;
        this.submodelRepository = submodelRepository;
    }

    @PostConstruct
    public void init() {
        new Thread(this).start();
    }

    protected void processSubmodelMessageEvent(SubmodelMessageEvent submodelMessageEvent) {
        var submodel = submodelMessageEvent.getSubmodel();
        var transformers = this.transformerJpaRepository.findAll();
        for (var transformer : transformers) {
            var transformerService = this.transformerServiceFactory.create(transformer);
            if (transformerService.isSubmodelSourceOfTransformerActions(submodel)) {
                switch (submodelMessageEvent.getChangeEventType()) {
                    case CREATED:
                    case UPDATED:
                        transformerService.execute(submodelMessageEvent.getSubmodel());
                        break;

                    case DELETED:
                        try {
                            this.submodelRepository.deleteSubmodel(transformer.getDestination().getSubmodelDestination().getId());
                        } catch (ElementDoesNotExistException e) {
                        } catch (Exception e) {
                            LOG.error(e.getMessage());
                        }
                        break;
                }
            }
        }
    }

}
