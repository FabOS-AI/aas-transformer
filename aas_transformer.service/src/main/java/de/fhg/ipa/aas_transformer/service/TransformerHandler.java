package de.fhg.ipa.aas_transformer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import jakarta.transaction.Transactional;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class TransformerHandler {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerHandler.class);
    private final SubmodelRegistry submodelRegistry;
    private final SubmodelRepository submodelRepository;
    private final TransformerJpaRepository transformerJpaRepository;
    private final TransformerServiceFactory transformerServiceFactory;

    public TransformerHandler(
            SubmodelRegistry submodelRegistry,
            SubmodelRepository submodelRepository,
            TransformerJpaRepository transformerJpaRepository,
            TransformerServiceFactory transformerServiceFactory) {
        this.submodelRegistry = submodelRegistry;
        this.submodelRepository = submodelRepository;
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
            var newTransformerOptional = this.transformerJpaRepository.findById(transformer.getId());
            if(newTransformerOptional.isPresent()) {
                var transformerService = this.transformerServiceFactory.create(newTransformerOptional.get());
                try {
                    var submodels = this.submodelRepository.getAllSubmodels();
                    for (var submodel : submodels) {
                        if (transformerService.isSubmodelSourceOfTransformerActions(submodel)) {
                            transformerService.execute(submodel);
                        }
                    }
                } catch (DeserializationException e) {
                    LOG.error(e.getMessage());
                }
            }
        }
    }

    public void deleteTransformer(UUID transformerId, Boolean doCleanup) {
        Optional<Transformer> optionalTransformer = this.transformerJpaRepository.findById(transformerId);
        if(optionalTransformer.isPresent()) {
            if(doCleanup) {
                var destination = optionalTransformer.get().getDestination();
                try {
                    var submodels = this.submodelRepository.getAllSubmodels();

                    for (var submodel : submodels) {
                        if (isDestinationSubmodel(submodel, destination.getSubmodelDestination().getId())) {
                            try {
                                this.submodelRepository.deleteSubmodel(submodel.getId());
                            } catch (ElementDoesNotExistException e) {
                            }
                        }
                    }
                } catch (DeserializationException e) {
                    throw new RuntimeException(e);
                }
            }

            this.transformerJpaRepository.deleteById(transformerId);
        }
    }

    private boolean isDestinationSubmodel(Submodel submodel, String destinationSubmodelId) {
        return destinationSubmodelId.equals(submodel.getId());
    }
}
