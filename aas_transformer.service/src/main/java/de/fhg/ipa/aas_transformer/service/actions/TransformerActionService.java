package de.fhg.ipa.aas_transformer.service.actions;

import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.basyx.aas.metamodel.map.descriptor.SubmodelDescriptor;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.vab.exception.provider.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionService.class);

    protected final AASManager aasManager;

    protected TransformerActionService(AASManager aasManager) {
        this.aasManager = aasManager;
    }

    public abstract void execute(IIdentifier sourceAASId, IIdentifier sourceSMId, IIdentifier destinationAASId, ISubmodel destinationSubmodel);

    protected boolean isSourceAvailable(IIdentifier aasIdentifier, SubmodelIdentifier sourceSubmodelIdentifier, String sourceSubmodelElementId) {
        try {
            if (aasManager.retrieveAAS(aasIdentifier) == null) {
                return false;
            }

            ISubmodel sourceSubmodel = getSourceSubmodel(aasIdentifier, sourceSubmodelIdentifier);

            if (sourceSubmodel == null) {
                return false;
            }

            if (sourceSubmodel.getSubmodelElement(sourceSubmodelElementId) == null) {
                return false;
            }
        } catch (NullPointerException | ResourceNotFoundException e) {
            return false;
        }

        return true;
    }

    protected ISubmodel getSourceSubmodel(IIdentifier aasIdentifier, SubmodelIdentifier sourceSubmodelIdentifier) {
        return aasManager.retrieveSubmodel(
                aasIdentifier,
                getSourceSubmodelIdentifier(aasIdentifier, sourceSubmodelIdentifier)
        );
    }

    protected IIdentifier getSourceSubmodelIdentifier(
            IIdentifier aasIdentifier,
            SubmodelIdentifier sourceSubmodelIdentifier
    ) {
        try {
            switch (sourceSubmodelIdentifier.getIdentifierType()) {
                case ID -> {
                    return new CustomId(sourceSubmodelIdentifier.getIdentifierValue());
                }

                case ID_SHORT -> {
                    Map<String, ISubmodel> submodels = aasManager.retrieveSubmodels(aasIdentifier);
                    return submodels.get(sourceSubmodelIdentifier.getIdentifierValue()).getIdentification();
                }
                case SEMANTIC_ID -> {
                    Map<String, ISubmodel> submodels = aasManager.retrieveSubmodels(aasIdentifier);
                    return submodels
                            .values()
                            .stream()
                            .filter(
                                    s -> s.getSemanticId()
                                            .getKeys()
                                            .stream()
                                            .filter(id -> id.getValue().equals(sourceSubmodelIdentifier.getIdentifierValue())).findFirst().isPresent()
                            ).findFirst()
                            .get().getIdentification();
                }
                default -> {
                    throw new ResourceNotFoundException("Submode '" + sourceSubmodelIdentifier + "' not found for AAS '" + aasIdentifier + "'");
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage());
            throw new ResourceNotFoundException("Submodel '" + sourceSubmodelIdentifier.getIdentifierValue() + "' not found for AAS '" + aasIdentifier + "'");
        }
    }

    public abstract TransformerAction getTransformerAction();

    public boolean isSubmodelSourceOfTransformerAction(SubmodelDescriptor submodelDescriptor) {
        var transformerAction = this.getTransformerAction();
        var submodelIdentifier = transformerAction.getSourceSubmodelIdentifier();
        var idValue = submodelIdentifier.getIdentifierValue();

        switch (submodelIdentifier.getIdentifierType()) {
            case ID -> {
                return submodelDescriptor.getIdentifier().getId().equals(idValue);
            }

            case ID_SHORT -> {
                return submodelDescriptor.getIdShort().equals(idValue);
            }

            case SEMANTIC_ID -> {
                if (submodelDescriptor.getSemanticId() != null){
                    return submodelDescriptor
                            .getSemanticId()
                            .getKeys()
                            .stream()
                            .filter(k -> k.getValue().equals(idValue))
                            .findFirst()
                            .isPresent();
                } else {
                    return false;
                }
            }

            default -> {
                return false;
            }
        }
    }

}
