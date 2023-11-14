package de.fhg.ipa.aas_transformer.service.actions;

import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import de.fhg.ipa.aas_transformer.model.TransformerActionSubmodelTemplate;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.apache.commons.lang3.NotImplementedException;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class TransformerActionServiceFactory {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionServiceFactory.class);

    private final TemplateRenderer templateRenderer;

    private final AASManager aasManager;

    public TransformerActionServiceFactory(TemplateRenderer templateRenderer, AASManager aasManager) {
        this.templateRenderer = templateRenderer;
        this.aasManager = aasManager;
    }

    public TransformerActionService create(TransformerAction transformerAction) {
        var actionType = transformerAction.getActionType();
        switch (actionType) {
            case COPY:
                return new TransformerActionCopyService((TransformerActionCopy) transformerAction, aasManager);

            case SUBMODEL_TEMPLATE:
                return new TransformerActionSubmodelTemplateService(
                        (TransformerActionSubmodelTemplate) transformerAction,
                        aasManager,
                        templateRenderer);

            default:
                throw new NotImplementedException("Unknown TransformerActionType '" + actionType + "' => Skipping TransformerAction");
        }
    }

}
