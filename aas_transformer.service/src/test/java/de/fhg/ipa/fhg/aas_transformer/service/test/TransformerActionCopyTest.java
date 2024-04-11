package de.fhg.ipa.fhg.aas_transformer.service.test;

import de.fhg.ipa.aas_transformer.model.*;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class TransformerActionCopyTest extends AbstractIT {

    private Transformer testTransformer;

    @BeforeAll
    public void beforeAll() throws IOException, DeserializationException {
        super.beforeAll();
        // Create Submodel:
        this.submodelRepository.createOrUpdateSubmodel(GenericTestConfig.getSimpleSubmodel());

        // Init testTransformer
        String idShort = "simple_transformed_submodel";
        this.testTransformer = new Transformer(new Destination(new DestinationSubmodel(idShort, idShort)));
        SubmodelId sourceSubmodelId = new SubmodelId(
                SubmodelIdType.ID_SHORT,
                GenericTestConfig.getSimpleSubmodel().getId()
        );
        ArrayList<TransformerAction> transformerActions = new ArrayList<>() {{
            add(new TransformerActionCopy(sourceSubmodelId, "SimpleProperty"));
            add(new TransformerActionCopy(sourceSubmodelId, "SimpleProperty2"));
        }};
        this.testTransformer.addTransformerActions(transformerActions);
    }

    @Nested
    @Order(10)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class createSubmodel {
        @Test
        @Order(10)
        public void executeTransformerWithCopyActions() throws IOException, DeserializationException {
            var transformerService = transformerServiceFactory.create(testTransformer);
            var sourceSubmodel = submodelRepository.getSubmodel(GenericTestConfig.getSimpleSubmodel().getId());

            transformerService.execute(sourceSubmodel);

            assertThatCode(() -> {
                var transformedSubmodel = submodelRepository
                        .getSubmodel(testTransformer.getDestination().getSubmodelDestination().getId());
                assertThat(transformedSubmodel).isNotNull();
            }).doesNotThrowAnyException();
        }

        @Test
        @Order(20)
        public void checkDestinationSubmodelElementsHaveCorrectKeysAndValues() throws IOException, DeserializationException {
            var destinationSubmodel = submodelRepository.getSubmodel(
                    testTransformer.getDestination().getSubmodelDestination().getId()
            );
            var destinationSubmodelElements = destinationSubmodel.getSubmodelElements();

            for (TransformerAction a : testTransformer.getTransformerActions()) {
                TransformerActionCopy copyAction = (TransformerActionCopy) a;
                String sourceSubmodelElementIdShort = copyAction.getSourceSubmodelElement();

                var sourceSubmodelElement = submodelRepository.getSubmodelElement(
                        GenericTestConfig.getSimpleSubmodel().getId(),
                        sourceSubmodelElementIdShort
                );

                assertThat(destinationSubmodelElements)
                        .extracting(SubmodelElement::getIdShort)
                        .contains(sourceSubmodelElementIdShort)
                        .usingRecursiveComparison();

                assertThat(destinationSubmodelElements)
                        .usingRecursiveComparison()
                        .comparingOnlyFields("value", "idShort")
                        .isEqualTo(sourceSubmodelElement);
            }
        }
    }

    @Nested
    @Order(20)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class updateSubmodel {
        @Test
        @Order(10)
        public void updateOfSubmodelElementValue() throws IOException, DeserializationException {
            var sourceSubmodel = submodelRepository.getSubmodel(GenericTestConfig.getSimpleSubmodel().getId());
            var submodelElementToUpdate = (Property) sourceSubmodel.getSubmodelElements().get(0);
            submodelElementToUpdate.setValue(UUID.randomUUID().toString());

            submodelRepository.createOrUpdateSubmodelElement(GenericTestConfig.getSimpleSubmodel().getId(),
                    submodelElementToUpdate.getIdShort(), submodelElementToUpdate);

            sourceSubmodel = submodelRepository.getSubmodel(GenericTestConfig.getSimpleSubmodel().getId());
            var transformerService = transformerServiceFactory.create(testTransformer);
            transformerService.execute(sourceSubmodel);

            var destinationSubmodelElementValue = (Property) submodelRepository.getSubmodelElement(
                    testTransformer.getDestination().getSubmodelDestination().getId(),
                    submodelElementToUpdate.getIdShort()
            );

            assertThat(destinationSubmodelElementValue.getValue()).isEqualTo(submodelElementToUpdate.getValue());
        }
    }
}
