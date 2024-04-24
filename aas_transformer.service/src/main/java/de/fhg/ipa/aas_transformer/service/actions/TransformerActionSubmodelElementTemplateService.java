package de.fhg.ipa.aas_transformer.service.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionSubmodelElementTemplate;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
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
        var submodelElementTemplateString = this.transformerAction.getSubmodelElementTemplate();
        submodelElementTemplateString = this.templateRenderer.render(submodelElementTemplateString, context);

        try {
            var jsonDeserializer = new JsonDeserializer();
            var newSubmodelElement = jsonDeserializer.read(submodelElementTemplateString, SubmodelElement.class);

            if (this.transformerAction.getDestinationSubmodelElementId() == "") {
                this.transformerAction.setDestinationSubmodelElementId(newSubmodelElement.getIdShort());
            }

            var optionalDestinationSubmodelElement = destinationSubmodel.getSubmodelElements().stream()
                    .filter(se -> se.getIdShort().equals(this.transformerAction.getDestinationSubmodelElementId())).findAny();
            if (optionalDestinationSubmodelElement.isPresent()) {
                var destinationSubmodelElement = optionalDestinationSubmodelElement.get();
                if (destinationSubmodelElement instanceof SubmodelElementList) {
                    var listValues = ((SubmodelElementList) destinationSubmodelElement).getValue();

                    var jsonSerializer = new JsonSerializer();
                    var objectMapper = new ObjectMapper();
                    var elementAlreadyExists = false;
                    for (int i = 0; i < listValues.size(); i++) {
                        try {
                            var listValueJson = jsonSerializer.write(listValues.get(i));

                            JsonNode valueAlreadyInList = objectMapper.readTree(listValueJson);
                            JsonNode valueToAddToList = objectMapper.readTree(submodelElementTemplateString);
                            if (valueAlreadyInList.equals(valueToAddToList)) {
                                listValues.remove(i);
                                listValues.add(i, newSubmodelElement);
                                elementAlreadyExists = true;
                                break;
                            }
                        } catch (SerializationException | IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    if (!elementAlreadyExists) {
                        listValues.add(newSubmodelElement);
                    }
                    ((SubmodelElementList) destinationSubmodelElement).setValue(listValues);
                    newSubmodelElement = destinationSubmodelElement;
                }
            }

            this.submodelRepository.createOrUpdateSubmodelElement(destinationSubmodel.getId(),
                    this.transformerAction.getDestinationSubmodelElementId(), newSubmodelElement);

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
