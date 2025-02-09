package de.fhg.ipa.aas_transformer.transformation.actions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionSubmodelElementTemplate;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.transformation.templating.TemplateRenderer;
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
import java.util.List;
import java.util.Map;

public class TransformerActionSubmodelElementTemplateService extends TransformerActionService {

    private static final Logger LOG = LoggerFactory.getLogger(TransformerActionSubmodelElementTemplateService.class);

    private final TransformerActionSubmodelElementTemplate transformerAction;

    private final TemplateRenderer templateRenderer;

    public TransformerActionSubmodelElementTemplateService(
            TransformerActionSubmodelElementTemplate transformerAction,
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
        var submodelElementTemplateString = this.transformerAction.getSubmodelElementTemplate();
        submodelElementTemplateString = this.templateRenderer.render(submodelElementTemplateString, context);

        try {
            var jsonDeserializer = new JsonDeserializer();
            var newSubmodelElement = jsonDeserializer.read(submodelElementTemplateString, SubmodelElement.class);

            if (this.transformerAction.getDestinationSubmodelElementId() == "") {
                this.transformerAction.setDestinationSubmodelElementId(newSubmodelElement.getIdShort());
            }

            var optionalDestinationSubmodelElement = intermediateResult.getSubmodelElements().stream()
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

            updateSubmodelElement(intermediateResult, newSubmodelElement);

            LOG.info("Transformer Action 'SUBMODEL_ELEMENT_TEMPLATE' executed | " +
                    "Source [SM='" + sourceSubmodel.getId() + "'] | " +
                    "Destination [SM='" + intermediateResult.getId() + "] | SME='" + newSubmodelElement.getIdShort() +"']");

            return intermediateResult;
        } catch (DeserializationException e) {
            LOG.error(e.getMessage());
        }
        return intermediateResult;
    }

    private void updateSubmodelElement(Submodel intermediateResult, SubmodelElement newSubmodelElement) {
        List<SubmodelElement> submodelElements = intermediateResult.getSubmodelElements();
        submodelElements.removeIf(se -> se.getIdShort().equals(newSubmodelElement.getIdShort()));
        submodelElements.add(newSubmodelElement);
        intermediateResult.setSubmodelElements(submodelElements);
    }

    @Override
    public TransformerAction getTransformerAction() {
        return this.transformerAction;
    }
}
