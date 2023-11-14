package de.fhg.ipa.fhg.aas_transformer.service.test;

import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.persistence.api.TransformerJpaRepository;
import de.fhg.ipa.aas_transformer.service.TransformerHandler;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.api.submodelelement.ISubmodelElement;
import org.junit.jupiter.api.*;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import static de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class TransformerActionCopyTest extends AbstractIT {

    private Transformer testTransformer;

    @BeforeAll
    public void beforeAll() throws IOException {
        super.beforeAll();
        // Create AAS and Submodel:
        this.aasManager.createAAS(GenericTestConfig.AAS, getAASServerUrl());
        this.aasManager.createSubmodel(
                GenericTestConfig.AAS.getIdentification(),
                getAnsibleFactsSubmodel()
        );

        // Init testTransformer
        String idShort = "operating_system";
        CustomId customId = new CustomId(idShort);

        this.testTransformer = new Transformer(new Destination(new DestinationSM(idShort, customId)));
        SubmodelIdentifier sourceSubmodelIdentifier = new SubmodelIdentifier(
                SubmodelIdentifierType.ID_SHORT,
                "ansible-facts"
        );
        ArrayList<TransformerAction> transformerActions = new ArrayList<>() {{
            add(new TransformerActionCopy(sourceSubmodelIdentifier,"distribution"));
            add(new TransformerActionCopy(sourceSubmodelIdentifier,"distribution_major_version"));
        }};
        this.testTransformer.addTransformerActions(transformerActions);
    }

    private ISubmodelElement getSourceSubmodelElement(IIdentifier aasId, IIdentifier submodelId, String submodelElementKey) {
        return this.aasManager
                .retrieveSubmodel(aasId, submodelId)
                .getSubmodelElement(submodelElementKey);
    }

    @Nested
    @Order(10)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class testCreateSubmodel {
        @Test
        @Order(10)
        public void testExecuteTransformerWithCopyActions() {
            var transformerService = transformerServiceFactory.create(testTransformer);
            transformerService.execute(GenericTestConfig.AAS.getIdentification());
        }

        @Test
        @Order(20)
        public void testCountOfSubmodelsExpectTwo() {
            Map<String, ISubmodel> submodelMap = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());

            assertEquals(2, submodelMap.size());
        }

        @Test
        @Order(30)
        public void testDestinationSubmodelElementsHaveCorrectKeysAndValues() throws IOException {
            var osSubmodel = aasManager.retrieveSubmodel(
                    AAS.getIdentification(),
                    testTransformer.getDestination().getSmDestination().getIdentifier()
            );
            var osSubmodelElements = osSubmodel.getSubmodelElements();


            for(TransformerAction a : testTransformer.getTransformerActions()) {
                TransformerActionCopy copyAction = (TransformerActionCopy) a;
                String sourceSubmodelElementKey = copyAction.getSourceSubmodelElement();

                var sourceSubmodelElement = getSourceSubmodelElement(
                        AAS.getIdentification(),
                        getAnsibleFactsSubmodel().getIdentification(),
                        sourceSubmodelElementKey
                );

                assertThat(osSubmodelElements)
                        .containsKey(sourceSubmodelElementKey)
                        .usingRecursiveComparison();

                assertThat(osSubmodelElements.get(sourceSubmodelElementKey))
                        .usingRecursiveComparison()
                        .comparingOnlyFields("value", "idShort")
                        .isEqualTo(sourceSubmodelElement);;
            }
        }
    }

    @Nested
    @Order(20)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class testSubmodelUpdate {
        String keyOfSubmodelElement = "distribution";
        String newValueOfSubmodelElement = "Windows";

        @Test
        @Order(10)
        public void testUpdateOfSubmodelElementValue() throws IOException {
            // Get ansible-facts submodel
            ISubmodel sourceSubmodel = aasManager.retrieveSubmodel(
                    AAS.getIdentification(),
                    getAnsibleFactsSubmodel().getIdentification()
            );

            // Change value of SubmodelElement with key 'distribution'
            sourceSubmodel.getSubmodelElement(keyOfSubmodelElement).setValue(newValueOfSubmodelElement);
        }

        @Test
        @Order(20)
        public void testRunTransformerAfterUpdate() {
            var transformerService = transformerServiceFactory.create(testTransformer);
            transformerService.execute(GenericTestConfig.AAS.getIdentification());
        }

        @Test
        @Order(30)
        public void testDestinationSubmodelHasUpdatedValue() throws IOException {
            var destinationSubmodelElementValue = getSourceSubmodelElement(
                    AAS.getIdentification(),
                    getAnsibleFactsSubmodel().getIdentification(),
                    keyOfSubmodelElement
            );

            assertThat(destinationSubmodelElementValue.getValue()).isEqualTo(newValueOfSubmodelElement);
        }
    }

}
