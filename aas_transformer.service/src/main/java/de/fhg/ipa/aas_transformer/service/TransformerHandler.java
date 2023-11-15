package de.fhg.ipa.aas_transformer.service;

import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.mqtt.MessageListener;
import de.fhg.ipa.aas_transformer.service.mqtt.ReceivedObject;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.eclipse.basyx.aas.metamodel.map.descriptor.AASDescriptor;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.basyx.aas.metamodel.map.descriptor.SubmodelDescriptor;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.vab.exception.provider.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;

@Component
public class TransformerHandler implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerHandler.class);
    private final AASRegistry aasRegistry;
    private final AASManager aasManager;
    private final TransformerJpaRepository transformerJpaRepository;
    private final TransformerServiceFactory transformerServiceFactory;

    public TransformerHandler(
            AASRegistry aasRegistry,
            AASManager aasManager,
            TransformerJpaRepository transformerJpaRepository,
            TransformerServiceFactory transformerServiceFactory) {
        this.aasRegistry = aasRegistry;
        this.aasManager = aasManager;
        this.transformerJpaRepository = transformerJpaRepository;
        this.transformerServiceFactory = transformerServiceFactory;
    }

    @PostConstruct
    public void init() {
        var thread = new Thread(this);
        thread.start();
    }

    public void saveTransformer(Transformer transformer, Boolean execute) {
        this.transformerJpaRepository.save(transformer);

        if(execute) {
            Optional<Transformer> newTransformer = this.transformerJpaRepository.findById(transformer.getId());

            if(newTransformer.isEmpty()) {
                return;
            }

            List<AASDescriptor> aasDescriptorCollection = this.aasRegistry.lookupAll();

            for(AASDescriptor aasDescriptor : aasDescriptorCollection) {
                var transformerService = this.transformerServiceFactory.create(newTransformer.get());
                transformerService.execute(aasDescriptor.getIdentifier());
            }
        }
    }

    public void deleteTransformer(UUID transformerId, Boolean doCleanup) {
        Optional<Transformer> optionalTransformer = this.transformerJpaRepository.findById(transformerId);

        if(optionalTransformer.isEmpty()) {
            return;
        }

        if(doCleanup) {
            List<AASDescriptor> aasDescriptorCollection = this.aasRegistry.lookupAll();
            var destinationSubmodel = optionalTransformer.get().getDestination();

            for (AASDescriptor aasDescriptor : aasDescriptorCollection) {
                this.cleanUpAasFromDestinationSubmodel(aasDescriptor.getIdentifier(), destinationSubmodel
                );
            }
        }

        this.transformerJpaRepository.deleteById(transformerId);
    }

    private void cleanUpAasFromDestinationSubmodel(IIdentifier aasId, Destination destination) {
        List<SubmodelDescriptor> submodelDescriptors = this.aasRegistry.lookupSubmodels(aasId);

        for(SubmodelDescriptor submodelDescriptor : submodelDescriptors) {
            if(isDestinationSubmodel(submodelDescriptor, destination.getSmDestination().getIdentifier())) {
                try {
                    this.aasManager.deleteSubmodel(
                            aasId,
                            submodelDescriptor.getIdentifier()
                    );
                } catch(ResourceNotFoundException e) {}
            }
        }

    }

    public void processSubmodelChangeNotification(
            IIdentifier aasId,
            Submodel submodel,
            MqttVerb verb
    ) {
        var transformers = this.transformerJpaRepository.findAll();
        for (var transformer : transformers) {
            var transformerService = this.transformerServiceFactory.create(transformer);
            if(transformerService.isSubmodelSourceOfTransformerActions(submodel)) {
                if(verb.equals(MqttVerb.DELETED)) {
                    try {
                        this.aasManager.deleteSubmodel(
                                aasId,
                                transformer.getDestination().getSmDestination().getIdentifier()
                        );
                    } catch (ResourceNotFoundException e) {}
                    catch (Exception e) {
                        LOG.error(e.getMessage());
                    }
                } else {

                    transformerService.execute(aasId);
                }
            }
        }
    }

    private boolean isDestinationSubmodel(SubmodelDescriptor submodelDescriptor, IIdentifier destinationSMIdentifier) {
        return destinationSMIdentifier.equals(submodelDescriptor.getIdentifier());
    }

    @Override
    public void run() {
        while(true) {
            try {
                var receivedObject = MessageListener.getMessageCache().poll();
                if (receivedObject != null) {
                    var aasId = new CustomId(receivedObject.getAASIdFromTopic());
                    this.processSubmodelChangeNotification(
                            aasId,
                            receivedObject.getSubmodelFromTopic(),
                            receivedObject.getVerbFromTopic()
                    );
                }
                else {
                    Thread.sleep(500);
                }
            } catch (Exception e) {
                LOG.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
