package de.fhg.ipa.fhg.aas_transformer.service.test;

import de.fhg.ipa.aas_transformer.model.*;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class TransformerActionSMETemplateTest extends AbstractIT {

    private Transformer testTransformer;

    @BeforeAll
    public void beforeAll() throws IOException, DeserializationException {
        super.beforeAll();
        // Create Submodel:
        this.submodelRepository.createOrUpdateSubmodel(GenericTestConfig.getSimpleSubmodel());
        this.assertSubmodelExists(GenericTestConfig.getSimpleSubmodel().getId());

        // Init testTransformer
        String idShort = "transformed_template_submodel";
        CustomId customId = new CustomId(idShort);

        this.testTransformer = new Transformer(new Destination(new DestinationSubmodel(idShort, idShort)));
        SubmodelId sourceSubmodelId = new SubmodelId(
                SubmodelIdType.ID_SHORT,
                GenericTestConfig.getSimpleSubmodel().getId()
        );
        ArrayList<TransformerAction> transformerActions = new ArrayList<>() {{
            add(new TransformerActionSubmodelElementTemplate(
                    sourceSubmodelId,
                    TransformerActionSMETemplateTestConfig.SUBMODEL_ELEMENT_TEMPLATE));
        }};
        this.testTransformer.addTransformerActions(transformerActions);
    }

    @Test
    @Order(10)
    public void executeTransformer() throws IOException, DeserializationException {
        var transformerService = transformerServiceFactory.create(testTransformer);
        var sourceSubmodel = submodelRepository.getSubmodel(GenericTestConfig.getSimpleSubmodel().getId());
        transformerService.execute(sourceSubmodel);

        this.assertSubmodelExists("transformed_template_submodel");

        var destinationSubmodel = submodelRepository.getSubmodel(
                testTransformer.getDestination().getSubmodelDestination().getId()
        );
        var destinationSubmodelElements = destinationSubmodel.getSubmodelElements();

        assertThat(destinationSubmodelElements).extracting(SubmodelElement::getIdShort).contains("TemplatedSimpleProperty");

        var sourceSubmodelElementForTemplating = (Property)sourceSubmodel.getSubmodelElements().stream()
                .filter(sme -> sme.getIdShort().equals("SimpleProperty2")).findAny().get();
        var templatedSubmodelElement = (Property)destinationSubmodelElements.stream()
                .filter(sme -> sme.getIdShort().equals("TemplatedSimpleProperty")).findAny().get();

        assertThat(templatedSubmodelElement.getValue()).isEqualTo(sourceSubmodelElementForTemplating.getValue());
    }

}
