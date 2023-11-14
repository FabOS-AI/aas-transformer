package de.fhg.ipa.aas_transformer.service.actions;

import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionService.class);

    protected final AASManager aasManager;

    protected TransformerActionService(AASManager aasManager) {
        this.aasManager = aasManager;
    }

    public abstract void execute(IIdentifier sourceAASId, ISubmodel destinationSubmodel);

    protected boolean isSourceAvailable(IIdentifier aasIdentifier, SubmodelIdentifier sourceSubmodelIdentifier, String sourceSubmodelElementId) {
        if(aasManager.retrieveAAS(aasIdentifier)==null)
            return false;

        ISubmodel sourceSubmodel = getSourceSubmodel(aasIdentifier, sourceSubmodelIdentifier);

        if(sourceSubmodel == null)
            return false;

        if(sourceSubmodel.getSubmodelElement(sourceSubmodelElementId)==null)
            return false;

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
        switch(sourceSubmodelIdentifier.getIdentifierType()) {
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
                return null;
            }
        }
    }

    public abstract TransformerAction getTransformerAction();

    public boolean isSubmodelSourceOfTransformerAction(Submodel submodel) {
        var transformerAction = this.getTransformerAction();
        var submodelIdentifier = transformerAction.getSourceSubmodelIdentifier();
        var idValue = submodelIdentifier.getIdentifierValue();

        switch (submodelIdentifier.getIdentifierType()) {
            case ID -> {
                return submodel.getIdentification().getId().equals(idValue);
            }

            case ID_SHORT -> {
                return submodel.getIdShort().equals(idValue);
            }

            case SEMANTIC_ID -> {
                return submodel
                        .getSemanticId()
                        .getKeys()
                        .stream()
                        .filter (k -> k.getValue().equals(idValue) )
                        .findFirst()
                        .isPresent();
            }

            default -> {
                return false;
            }
        }
    }

}
