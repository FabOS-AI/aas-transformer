package de.fhg.ipa.aas_transformer.transformation.actions;

import de.fhg.ipa.aas_transformer.model.actions.TransformerAction;
import de.fhg.ipa.aas_transformer.model.actions.TransformerActionSmeRename;
import org.eclipse.digitaltwin.aas4j.v3.model.LangStringNameType;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultLangStringNameType;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultLangStringTextType;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.eclipse.digitaltwin.basyx.submodelservice.pathparsing.HierarchicalSubmodelElementParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

public class TransformerActionSmeRenameService extends TransformerActionService {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionSmeRenameService.class);
    private final TransformerActionSmeRename transformerAction;

    public TransformerActionSmeRenameService(TransformerActionSmeRename transformerAction) {
        this.transformerAction = transformerAction;
    }

    @Override
    public Submodel execute(
            Submodel sourceSubmodel,
            Submodel intermediateResult,
            Map<String, Object> context,
            boolean isFirstAction
    ) {
        HierarchicalSubmodelElementParser parser = new HierarchicalSubmodelElementParser(intermediateResult);
        SubmodelElement submodelElement;
        try {
            submodelElement = parser.getSubmodelElementFromIdShortPath(transformerAction.getSmePath());
        } catch(ElementDoesNotExistException e) {
            LOG.error("Can not rename submodel element '" + transformerAction.getSmePath()
                    + "', because submodel element was not found");
            return intermediateResult;
        }


        switch (transformerAction.getSubmodelElementProperty()) {
            case ID_SHORT -> submodelElement.setIdShort(transformerAction.getNewValue());
            case DISPLAY_NAME -> submodelElement.setDisplayName(List.of(createLangString()));
            case DESCRIPTION -> submodelElement.setDescription(List.of(createLangStringText()));
            default -> LOG.error("Property {} not supported for renaming in TransformerActionSmeRenameService",
                    transformerAction.getSubmodelElementProperty());
        }
        return intermediateResult;
    }

    private DefaultLangStringNameType createLangString() {
        DefaultLangStringNameType langString = new DefaultLangStringNameType();
        langString.setText(transformerAction.getNewValue());
        return langString;
    }

    private DefaultLangStringTextType createLangStringText() {
        DefaultLangStringTextType langString = new DefaultLangStringTextType();
        langString.setText(transformerAction.getNewValue());
        return langString;
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
