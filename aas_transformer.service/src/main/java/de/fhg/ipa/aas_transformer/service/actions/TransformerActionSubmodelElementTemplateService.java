package de.fhg.ipa.aas_transformer.service.actions;

import com.digitalpetri.netty.fsm.Event;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionSubmodelElementTemplate;
import de.fhg.ipa.aas_transformer.model.TransformerActionSubmodelTemplate;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.connected.ConnectedSubmodel;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TransformerActionSubmodelElementTemplateService extends TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionSubmodelElementTemplateService.class);

    private final TransformerActionSubmodelElementTemplate transformerAction;

    private final TemplateRenderer templateRenderer;

    public TransformerActionSubmodelElementTemplateService(
            TransformerActionSubmodelElementTemplate transformerAction,
            AASManager aasManager,
            TemplateRenderer templateRenderer) {
        super(aasManager);
        this.transformerAction = transformerAction;
        this.templateRenderer = templateRenderer;
    }

    @Override
    public void execute(IIdentifier sourceAASId, IIdentifier sourceSMId, IIdentifier destinationAASId, ISubmodel destinationSubmodel) {
        var renderContext = new HashMap<String, Object>();
        renderContext.put("SOURCE_AAS", sourceAASId.getId());
        if (sourceSMId != null) {
            renderContext.put("SOURCE_SM", sourceSMId);
        }

        var objectMapper = new ObjectMapper();

        var submodelElementTemplateString = transformerAction.getSubmodelElementTemplate();
        submodelElementTemplateString = this.templateRenderer.render(submodelElementTemplateString, renderContext);

        var existingSubmodel = aasManager.retrieveSubmodel(destinationAASId, destinationSubmodel.getIdentification());
        var localExistingSubmodel = ((ConnectedSubmodel)existingSubmodel).getLocalCopy();

        try {
            var newSubmodelElement = objectMapper.readValue(submodelElementTemplateString, SubmodelElement.class);
            localExistingSubmodel.addSubmodelElement(newSubmodelElement);
            aasManager.createSubmodel(destinationAASId, localExistingSubmodel);
            LOG.info("Transformer Action 'SUBMODEL_ELEMENT_TEMPLATE' executed | " +
                    "Source [aas='" + sourceAASId.getId() + "' | SM='" + sourceSMId.getId() + "'] | " +
                    "Destination [aas='" + destinationAASId.getId() + "' | SM='" + destinationSubmodel.getIdentification().getId() + "]");

        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
