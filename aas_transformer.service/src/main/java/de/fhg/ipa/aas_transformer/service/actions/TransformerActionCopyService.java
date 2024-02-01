package de.fhg.ipa.aas_transformer.service.actions;

import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.connected.submodelelement.ConnectedSubmodelElementCollection;
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

        if (isSourceAvailable(sourceAASId, transformerAction.getSourceSubmodelIdentifier(), sourceSubmodelElementId) == false) {
            LOG.warn("Source [aasId='" + sourceAASId + "', smId='" + sourceSMId + "'] not available anymore => Skip action");
            return;
        }

        var submodelElementToCopy = getSourceSubmodel(sourceAASId, transformerAction.getSourceSubmodelIdentifier())
                .getSubmodelElement(sourceSubmodelElementId)
                .getLocalCopy();

        // If destinationSubmodelElement not set copy it to the highest level of the submodel
        try {
            if (transformerAction.getDestinationSubmodelElement() == null) {
                destinationSubmodel.addSubmodelElement(submodelElementToCopy);
            } else {
                var destinationSubmodelElementId = transformerAction.getDestinationSubmodelElement();
                if (destinationSubmodelElementId.contains("/")) {
                    var destinationSubmodelElement = (ConnectedSubmodelElementCollection) destinationSubmodel.getSubmodelElement(destinationSubmodelElementId);
                    submodelElementToCopy.setParent(destinationSubmodelElement.getReference());
                    destinationSubmodelElement.addSubmodelElement(submodelElementToCopy);
                }
                LOG.info("Transformer Action 'COPY' executed | " +
                        "Source [aas='" + sourceAASId.getId() + "' | SM='" + sourceSMId.getId() + "'] | " +
                        "Destination [aas='" + destinationAASId.getId() + "' | SM='" + destinationSubmodel.getIdentification().getId() + "]");
            }
        } catch (ResourceNotFoundException e) {
            LOG.error("Can not copy submodel element to target element '" + transformerAction.getDestinationSubmodelElement()
                    + "', because the destination submodel element was not found");
        }
        catch (Exception e) {
            LOG.error("Can not copy submodel element to target element: " + e.getMessage());
        }
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
