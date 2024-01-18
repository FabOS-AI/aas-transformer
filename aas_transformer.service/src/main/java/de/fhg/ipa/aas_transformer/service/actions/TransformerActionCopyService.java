package de.fhg.ipa.aas_transformer.service.actions;

import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;
import org.eclipse.basyx.vab.exception.provider.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransformerActionCopyService extends TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionCopyService.class);

    private final TransformerActionCopy transformerAction;

    public TransformerActionCopyService(TransformerActionCopy transformerAction, AASManager aasManager) {
        super(aasManager);
        this.transformerAction = transformerAction;
    }

    @Override
    public void execute(IIdentifier sourceAASId, IIdentifier destinationAASId, ISubmodel destinationSubmodel) {
        var sourceSubmodelElementId = transformerAction.getSourceSubmodelElement();

        if(isSourceAvailable(sourceAASId, transformerAction.getSourceSubmodelIdentifier(), sourceSubmodelElementId)==false) {
            LOG.warn("Source not available anymore => Skip action");
            return;
        }

        SubmodelElement destinationSubmodelElement =
                getSourceSubmodel(
                        sourceAASId,
                        transformerAction.getSourceSubmodelIdentifier()
                )
                        .getSubmodelElement(sourceSubmodelElementId)
                        .getLocalCopy();

        try {
            destinationSubmodel
                    .getSubmodelElement(sourceSubmodelElementId)
                    .setValue(destinationSubmodelElement.getValue());
            LOG.info("Update SubmodelElement with key = '"+ sourceSubmodelElementId +"' to value = '"+ destinationSubmodelElement.getValue() +"'");
        } catch(ResourceNotFoundException e) {
            destinationSubmodel.addSubmodelElement(destinationSubmodelElement);
            LOG.info("Create SubmodelElement with key = '"+ sourceSubmodelElementId +"' and value = '"+ destinationSubmodelElement.getValue() +"'");
        }
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
