package de.fhg.ipa.aas_transformer.transformation.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionSubmodelTemplate;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TransformerActionSubmodelTemplateService extends TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionSubmodelTemplateService.class);

    private final TransformerActionSubmodelTemplate transformerAction;

    private final TemplateRenderer templateRenderer;

    public TransformerActionSubmodelTemplateService(
            TransformerActionSubmodelTemplate transformerAction,
            TemplateRenderer templateRenderer
    ) {
        this.transformerAction = transformerAction;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public Submodel execute(
            Submodel sourceSubmodel,
            Submodel intermediateResult,
            Map<String, Object> context,
            boolean isFirstAction
    ) {
        var submodelTemplateString = transformerAction.getSubmodelTemplate();
        submodelTemplateString = this.templateRenderer.render(submodelTemplateString, context);

        try {
            var jsonDeserializer = new JsonDeserializer();
            var newDestinationSubmodel = jsonDeserializer.read(submodelTemplateString, Submodel.class);
            newDestinationSubmodel.setIdShort(intermediateResult.getIdShort());
            newDestinationSubmodel.setId(intermediateResult.getId());

//            this.submodelRepository.createOrUpdateSubmodel(newDestinationSubmodel);
            LOG.info("Transformer Action 'SUBMODEL_TEMPLATE' executed | " +
                    "Source [SM='" + sourceSubmodel.getId() + "'] | " +
                    "Destination [SM='" + intermediateResult.getId() + "]");

            return newDestinationSubmodel;
        } catch (DeserializationException e) {
            LOG.error(e.getMessage());
        }

        return intermediateResult;
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
