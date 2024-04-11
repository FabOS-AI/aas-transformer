package de.fhg.ipa.aas_transformer.service.actions;

import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import de.fhg.ipa.aas_transformer.model.TransformerActionSubmodelElementTemplate;
import de.fhg.ipa.aas_transformer.model.TransformerActionSubmodelTemplate;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.apache.commons.lang3.NotImplementedException;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TransformerActionServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionServiceFactory.class);

    private final TemplateRenderer templateRenderer;

    private final SubmodelRegistry submodelRegistry;

    private final SubmodelRepository submodelRepository;

    public TransformerActionServiceFactory(TemplateRenderer templateRenderer,
                                           SubmodelRegistry submodelRegistry,
                                           SubmodelRepository submodelRepository) {
        this.templateRenderer = templateRenderer;
        this.submodelRegistry = submodelRegistry;
        this.submodelRepository = submodelRepository;
    }

    public TransformerActionService create(TransformerAction transformerAction) {
        var actionType = transformerAction.getActionType();
        switch (actionType) {
            case COPY:
                return new TransformerActionCopyService((TransformerActionCopy) transformerAction,
                        this.submodelRegistry, this.submodelRepository);

            case SUBMODEL_TEMPLATE:
                return new TransformerActionSubmodelTemplateService(
                        (TransformerActionSubmodelTemplate) transformerAction,
                        this.submodelRegistry, this.submodelRepository,
                        this.templateRenderer);

            case SUBMODEL_ELEMENT_TEMPLATE:
                return new TransformerActionSubmodelElementTemplateService(
                        (TransformerActionSubmodelElementTemplate) transformerAction,
                        this.submodelRegistry, this.submodelRepository,
                        templateRenderer);

            default:
                throw new NotImplementedException("Unknown TransformerActionType '" + actionType + "' => Skipping TransformerAction");
        }
    }

}
