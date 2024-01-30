package de.fhg.ipa.aas_transformer.service.events.consumers;

import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.TransformerServiceFactory;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.events.SubmodelMessageEvent;
import org.eclipse.basyx.vab.exception.provider.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;

public abstract class MessageEventConsumer implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(MessageEventConsumer.class);

    protected final AASManager aasManager;

    protected final AASRegistry aasRegistry;

    protected final TransformerServiceFactory transformerServiceFactory;

    protected final TransformerJpaRepository transformerJpaRepository;

    protected MessageEventConsumer(AASManager aasManager,
                                   AASRegistry aasRegistry,
                                   TransformerServiceFactory transformerServiceFactory,
                                   TransformerJpaRepository transformerJpaRepository) {
        this.aasManager = aasManager;
        this.aasRegistry = aasRegistry;
        this.transformerServiceFactory = transformerServiceFactory;
        this.transformerJpaRepository = transformerJpaRepository;
    }

    @PostConstruct
    public void init() {
        new Thread(this).start();
    }

    protected void processSubmodelMessageEvent(SubmodelMessageEvent submodelMessageEvent) {
        var submodelDescriptor = submodelMessageEvent.getSubmodelDescriptor();
        var transformers = this.transformerJpaRepository.findAll();
        for (var transformer : transformers) {
            var transformerService = this.transformerServiceFactory.create(transformer);
            if (transformerService.isSubmodelSourceOfTransformerActions(submodelDescriptor)) {
                switch (submodelMessageEvent.getChangeEventType()) {
                    case CREATED:
                    case UPDATED:
                        transformerService.execute(submodelMessageEvent.getAasId(), submodelMessageEvent.getSubmodelDescriptor().getIdentifier());
                        break;

                    case DELETED:
                        try {
                            this.aasManager.deleteSubmodel(
                                    submodelMessageEvent.getAasId(),
                                    transformer.getDestination().getSmDestination().getIdentifier()
                            );
                        } catch (ResourceNotFoundException e) {
                        } catch (Exception e) {
                            LOG.error(e.getMessage());
                        }
                        break;
                }
            }
        }
    }

}
