package de.fhg.ipa.aas_transformer.service.actions;

import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.connected.submodelelement.ConnectedSubmodelElementCollection;
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
    public void execute(IIdentifier sourceAASId, IIdentifier sourceSMId, IIdentifier destinationAASId, ISubmodel destinationSubmodel) {
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

        String targetSubmodelElementId;
        // If destinationSubmodelElement not set use same submodel element as in source submodel
        if (transformerAction.getDestinationSubmodelElement() == null) {
            targetSubmodelElementId = sourceSubmodelElementId;
        } else {
            targetSubmodelElementId = transformerAction.getDestinationSubmodelElement();
        }

        try {
            if (targetSubmodelElementId.contains("/")) {
                var targetSubmodelElement = (ConnectedSubmodelElementCollection)destinationSubmodel.getSubmodelElement(targetSubmodelElementId);
                destinationSubmodelElement.setParent(targetSubmodelElement.getReference());
                targetSubmodelElement.addSubmodelElement(destinationSubmodelElement);
            }
            else {
                var targetSubmodelElement = destinationSubmodel.getSubmodelElement(targetSubmodelElementId);
                targetSubmodelElement.setValue(targetSubmodelElement.getValue());
            }
            LOG.info("Update SubmodelElement with key = '"+ targetSubmodelElementId +"' to value = '"+ destinationSubmodelElement.getValue() +"'");
        } catch(ResourceNotFoundException e) {
            LOG.error("Can not copy submodel element to target element '" + targetSubmodelElementId+ "', because the target element was not found");
        }
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
