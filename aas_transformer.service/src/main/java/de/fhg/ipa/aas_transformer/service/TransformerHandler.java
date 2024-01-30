package de.fhg.ipa.aas_transformer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import jakarta.transaction.Transactional;
import org.eclipse.basyx.aas.metamodel.map.descriptor.AASDescriptor;
import org.eclipse.basyx.aas.metamodel.map.descriptor.SubmodelDescriptor;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.vab.exception.provider.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class TransformerHandler {
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

    @Autowired
    ObjectMapper testObjectMapper;

    @Transactional
    public void createOrUpdateTransformer(Transformer transformer, Boolean execute) {
        var savedTransformerOptional = transformerJpaRepository.findById(transformer.getId());
        if (savedTransformerOptional.isPresent()) {
            savedTransformerOptional.get().setDestination(transformer.getDestination());
            savedTransformerOptional.get().setTransformerActions(transformer.getTransformerActions());
            LOG.info("Transformer '" + transformer.getId() + "' updated");
        }
        else {
            var savedTransformer = this.transformerJpaRepository.save(transformer);
            LOG.info("Transformer '" + transformer.getId() + "' created");
        }

        if(execute) {
            Optional<Transformer> newTransformer = this.transformerJpaRepository.findById(transformer.getId());

            if(newTransformer.isEmpty()) {
                return;
            }

            List<AASDescriptor> aasDescriptorCollection = this.aasRegistry.lookupAll();

            for(AASDescriptor aasDescriptor : aasDescriptorCollection) {
                var transformerService = this.transformerServiceFactory.create(newTransformer.get());
                transformerService.execute(aasDescriptor.getIdentifier(), null);
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
                this.cleanUpAasFromDestinationSubmodel(aasDescriptor.getIdentifier(), destinationSubmodel);
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

    private boolean isDestinationSubmodel(SubmodelDescriptor submodelDescriptor, IIdentifier destinationSMIdentifier) {
        return destinationSMIdentifier.equals(submodelDescriptor.getIdentifier());
    }
}
