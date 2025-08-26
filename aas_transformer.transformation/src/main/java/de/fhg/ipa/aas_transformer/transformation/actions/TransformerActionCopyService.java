package de.fhg.ipa.aas_transformer.transformation.actions;

import de.fhg.ipa.aas_transformer.model.actions.TransformerAction;
import de.fhg.ipa.aas_transformer.model.actions.TransformerActionCopy;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.eclipse.digitaltwin.basyx.submodelservice.pathparsing.HierarchicalSubmodelElementParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class TransformerActionCopyService extends TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionCopyService.class);

    private final TransformerActionCopy transformerAction;

    public TransformerActionCopyService(
            TransformerActionCopy transformerAction
    ) {
        this.transformerAction = transformerAction;
    }

    @Override
    public Submodel execute(
            Submodel sourceSubmodel,
            Submodel intermediateResult,
            Map<String, Object> context,
            boolean isFirstAction
    ) {
        var sourceSubmodelElementId = transformerAction.getSourceSubmodelElement();
        SubmodelElement submodelElementToCopy;
        try {
            HierarchicalSubmodelElementParser parser = new HierarchicalSubmodelElementParser(sourceSubmodel);
            submodelElementToCopy = parser.getSubmodelElementFromIdShortPath(sourceSubmodelElementId);
        } catch (ElementDoesNotExistException e) {
            LOG.error("Can not copy source submodel element '" + transformerAction.getSourceSubmodelElement()
                    + "', because source submodel element was not found");
            throw new ElementDoesNotExistException(transformerAction.getSourceSubmodelElement());
        }

        String destinationSubmodelElementId = transformerAction.getDestinationSubmodelElement();
        submodelElementToCopy.setIdShort(destinationSubmodelElementId);

        SubmodelElement destinationSubmodelElement;
//        try {
//            HierarchicalSubmodelElementParser parser = new HierarchicalSubmodelElementParser(intermediateResult);
//            destinationSubmodelElement = parser.getSubmodelElementFromIdShortPath(destinationSubmodelElementId);
//            destinationSubmodelElement = submodelElementToCopy;
//        } catch (ElementDoesNotExistException e) {
//            LOG.info("Destination Submodel Element with ID {} has to be created, because it was not found in Submodel with ID {}",
//                    transformerAction.getDestinationSubmodelElement(),
//                    intermediateResult.getId()
//            );

        List<SubmodelElement> submodels = intermediateResult.getSubmodelElements();
        submodels.add(submodelElementToCopy);
        intermediateResult.setSubmodelElements(submodels);
//        }

        LOG.info("Transformer Action 'COPY' executed | Source Submodel [id='{}'] | Destination Submodel [id='{}]",
                sourceSubmodel.getId(),
                intermediateResult.getId()
        );

        return intermediateResult;
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
