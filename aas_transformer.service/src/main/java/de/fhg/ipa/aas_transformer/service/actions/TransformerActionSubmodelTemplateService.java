package de.fhg.ipa.aas_transformer.service.actions;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionSubmodelTemplate;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class TransformerActionSubmodelTemplateService extends TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionSubmodelTemplateService.class);

    private final TransformerActionSubmodelTemplate transformerAction;

    private final TemplateRenderer templateRenderer;

    public TransformerActionSubmodelTemplateService(
            TransformerActionSubmodelTemplate transformerAction,
            AASManager aasManager,
            TemplateRenderer templateRenderer) {
        super(aasManager);
        this.transformerAction = transformerAction;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public void execute(IIdentifier sourceAASId, ISubmodel destinationSubmodel) {
        Map<String, Object> renderContext = Map.of(
                "SOURCE_AAS", sourceAASId.getId()
        );

        var objectMapper = new ObjectMapper();

        var submodelTemplateString = transformerAction.getSubmodelTemplate();
        submodelTemplateString = this.templateRenderer.render(submodelTemplateString, renderContext);

        Submodel newDestinationSubmodel = null;
        try {
            newDestinationSubmodel = objectMapper.readValue(submodelTemplateString, Submodel.class);
            for (var sme : newDestinationSubmodel.getSubmodelElements().entrySet()) {
                var newSubmodelElement = SubmodelElement.createAsFacade((Map<String, Object>) sme.getValue());
                newDestinationSubmodel.getSubmodelElements().put(sme.getKey(), newSubmodelElement);
            }
            newDestinationSubmodel.setIdShort(destinationSubmodel.getIdShort());
            newDestinationSubmodel.setIdentification(destinationSubmodel.getIdentification());

            this.aasManager.createSubmodel(sourceAASId, newDestinationSubmodel);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
