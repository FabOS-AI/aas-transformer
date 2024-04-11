package de.fhg.ipa.aas_transformer.service.actions;

import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionSubmodelElementTemplate;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TransformerActionSubmodelElementTemplateService extends TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionSubmodelElementTemplateService.class);

    private final TransformerActionSubmodelElementTemplate transformerAction;

    private final TemplateRenderer templateRenderer;

    public TransformerActionSubmodelElementTemplateService(
            TransformerActionSubmodelElementTemplate transformerAction,
            SubmodelRegistry submodelRegistry, SubmodelRepository submodelRepository,
            TemplateRenderer templateRenderer) {
        super(submodelRegistry, submodelRepository);
        this.transformerAction = transformerAction;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public void execute(Submodel sourceSubmodel, Submodel destinationSubmodel, Map<String, Object> context) {
        var submodelElementTemplateString = transformerAction.getSubmodelElementTemplate();
        submodelElementTemplateString = this.templateRenderer.render(submodelElementTemplateString, context);

        try {
            var jsonDeserializer = new JsonDeserializer();
            var newSubmodelElement = jsonDeserializer.read(submodelElementTemplateString, SubmodelElement.class);
            this.submodelRepository.createOrUpdateSubmodelElement(destinationSubmodel.getId(), newSubmodelElement.getIdShort(), newSubmodelElement);

            LOG.info("Transformer Action 'SUBMODEL_ELEMENT_TEMPLATE' executed | " +
                    "Source [SM='" + sourceSubmodel.getId() + "'] | " +
                    "Destination [SM='" + destinationSubmodel.getId() + "] | SME='" + newSubmodelElement.getIdShort() +"']");
        } catch (DeserializationException e) {
            LOG.error(e.getMessage());
        }
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
