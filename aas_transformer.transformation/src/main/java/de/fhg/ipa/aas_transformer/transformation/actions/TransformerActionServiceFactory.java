package de.fhg.ipa.aas_transformer.transformation.actions;

import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.model.actions.*;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TransformerActionServiceFactory {
    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionServiceFactory.class);

    private final TemplateRenderer templateRenderer;
    private final SubmodelRegistry submodelRegistry;
    private final SubmodelRepository submodelRepository;

    public TransformerActionServiceFactory(
            TemplateRenderer templateRenderer,
            SubmodelRegistry submodelRegistry,
            SubmodelRepository submodelRepository
    ) {
        this.templateRenderer = templateRenderer;
        this.submodelRegistry = submodelRegistry;
        this.submodelRepository = submodelRepository;
    }

    public TransformerActionService create(TransformerAction transformerAction) {
        var actionType = transformerAction.getActionType();
        return switch (actionType) {
            case COPY -> new TransformerActionCopyService((TransformerActionCopy) transformerAction);
            case SME_RENAME -> new TransformerActionSmeRenameService((TransformerActionSmeRename) transformerAction);
            case SM_COPY -> new TransformerActionSmCopyService(
                    this.submodelRegistry,
                    this.submodelRepository,
                    this.templateRenderer,
                    (TransformerActionSmCopy) transformerAction
            );
            case SUBMODEL_TEMPLATE -> new TransformerActionSubmodelTemplateService(
                    (TransformerActionSubmodelTemplate) transformerAction,
                    this.templateRenderer
            );
            case SUBMODEL_ELEMENT_TEMPLATE -> new TransformerActionSubmodelElementTemplateService(
                    (TransformerActionSubmodelElementTemplate) transformerAction,
                    templateRenderer
            );
            case TS_AVG -> new TransformerActionTsAvgService(
                    (TransformerActionTsAvg) transformerAction
            );
            case TS_MDN -> new TransformerActionTsMdnService(
                    (TransformerActionTsMdn) transformerAction
            );
            case TS_TAKE_EVERY, TS_DROP_EVERY -> new TransformerActionTsReduceService(
                    (TransformerActionTsReduce) transformerAction
            );
            default ->
                    throw new NotImplementedException("Unknown TransformerActionType '" + actionType + "' => Skipping TransformerAction");
        };
    }

}
