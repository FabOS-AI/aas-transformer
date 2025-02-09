package de.fhg.ipa.aas_transformer.transformation.actions;

import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
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
        switch (actionType) {
            case COPY:
                return new TransformerActionCopyService((TransformerActionCopy) transformerAction);

            case SUBMODEL_TEMPLATE:
                return new TransformerActionSubmodelTemplateService(
                        (TransformerActionSubmodelTemplate) transformerAction,
                        this.templateRenderer
                );

            case SUBMODEL_ELEMENT_TEMPLATE:
                return new TransformerActionSubmodelElementTemplateService(
                        (TransformerActionSubmodelElementTemplate) transformerAction,
                        templateRenderer
                );

            case TS_AVG:
                return new TransformerActionTsAvgService(
                        (TransformerActionTsAvg) transformerAction
                );

            case TS_MDN:
                return new TransformerActionTsMdnService(
                        (TransformerActionTsMdn) transformerAction
                );

            case TS_TAKE_EVERY:
            case TS_DROP_EVERY:
                return new TransformerActionTsReduceService(
                        (TransformerActionTsReduce) transformerAction
                );

            default:
                throw new NotImplementedException("Unknown TransformerActionType '" + actionType + "' => Skipping TransformerAction");
        }
    }

}
