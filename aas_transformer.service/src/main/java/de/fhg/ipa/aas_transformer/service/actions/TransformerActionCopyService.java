package de.fhg.ipa.aas_transformer.service.actions;

import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TransformerActionCopyService extends TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionCopyService.class);

    private final TransformerActionCopy transformerAction;

    public TransformerActionCopyService(TransformerActionCopy transformerAction,
                                        SubmodelRegistry submodelRegistry, SubmodelRepository submodelRepository) {
        super(submodelRegistry, submodelRepository);
        this.transformerAction = transformerAction;
    }

    @Override
    public void execute(Submodel sourceSubmodel, Submodel destinationSubmodel, Map<String, Object> context) {
        var sourceSubmodelElementId = transformerAction.getSourceSubmodelElement();

        var submodelElementToCopyOptional =
                sourceSubmodel.getSubmodelElements()
                        .stream().filter(se -> se.getIdShort().equals(sourceSubmodelElementId))
                        .findAny();
        if (submodelElementToCopyOptional.isEmpty()) {
            LOG.error("Can not copy source submodel element '" + transformerAction.getSourceSubmodelElement()
                    + "', because source submodel element was not found");
            throw new ElementDoesNotExistException(transformerAction.getSourceSubmodelElement());
        }

        // If destinationSubmodelElement not set copy it to the highest level of the submodel
        try {
            if (transformerAction.getDestinationSubmodelElement() == null) {
                this.submodelRepository.createOrUpdateSubmodelElement(destinationSubmodel.getId(),
                        submodelElementToCopyOptional.get().getIdShort(), submodelElementToCopyOptional.get());
            } else {
                var destinationSubmodelElementId = transformerAction.getDestinationSubmodelElement();
                var destinationSubmodelElement = this.submodelRepository.getSubmodelElement(destinationSubmodel.getId(), destinationSubmodelElementId);

                if (destinationSubmodelElement instanceof SubmodelElementCollection) {
                    var destinationSubmodelElementCollection = (SubmodelElementCollection) destinationSubmodelElement;
                    var existingSubmodelElements = destinationSubmodelElementCollection.getValue();
                    existingSubmodelElements.add(submodelElementToCopyOptional.get());
                    destinationSubmodelElementCollection.setValue(existingSubmodelElements);
                    destinationSubmodelElement = destinationSubmodelElementCollection;
                } else if (destinationSubmodelElement instanceof SubmodelElementList) {
                    var destinaionSubmodelElementList = (SubmodelElementList) destinationSubmodel;
                    var existingSubmodelElements = destinaionSubmodelElementList.getValue();
                    existingSubmodelElements.add(submodelElementToCopyOptional.get());
                    destinaionSubmodelElementList.setValue(existingSubmodelElements);
                    destinationSubmodelElement = destinaionSubmodelElementList;
                }
                this.submodelRepository.updateSubmodelElement(destinationSubmodel.getId(), destinationSubmodelElementId, destinationSubmodelElement);

                LOG.info("Transformer Action 'COPY' executed | " +
                        "Source Submodel [id='" + sourceSubmodel.getId() + "'] | " +
                        "Destination [id='" + destinationSubmodel.getId() + "]");
            }
        } catch (ElementDoesNotExistException e) {
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
