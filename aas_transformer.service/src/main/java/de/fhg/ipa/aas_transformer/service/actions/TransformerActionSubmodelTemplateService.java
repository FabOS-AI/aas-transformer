package de.fhg.ipa.aas_transformer.service.actions;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionSubmodelTemplate;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.util.Map;

public class TransformerActionSubmodelTemplateService extends TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionSubmodelTemplateService.class);

    private final TransformerActionSubmodelTemplate transformerAction;

    private final TemplateRenderer templateRenderer;

    public TransformerActionSubmodelTemplateService(
            TransformerActionSubmodelTemplate transformerAction,
            SubmodelRegistry submodelRegistry, SubmodelRepository submodelRepository,
            TemplateRenderer templateRenderer) {
        super(submodelRegistry, submodelRepository);
        this.transformerAction = transformerAction;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public void execute(Submodel sourceSubmodel, Submodel destinationSubmodel, Map<String, Object> context) {
        var submodelTemplateString = transformerAction.getSubmodelTemplate();
        submodelTemplateString = this.templateRenderer.render(submodelTemplateString, context);

        try {
            var jsonDeserializer = new JsonDeserializer();
            var newDestinationSubmodel = jsonDeserializer.read(submodelTemplateString, Submodel.class);
            newDestinationSubmodel.setIdShort(destinationSubmodel.getIdShort());
            newDestinationSubmodel.setId(destinationSubmodel.getId());

            this.submodelRepository.createOrUpdateSubmodel(newDestinationSubmodel);
            LOG.info("Transformer Action 'SUBMODEL_TEMPLATE' executed | " +
                    "Source [SM='" + sourceSubmodel.getId() + "'] | " +
                    "Destination [SM='" + destinationSubmodel.getId() + "]");
        } catch (DeserializationException e) {
            LOG.error(e.getMessage());
        }
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
