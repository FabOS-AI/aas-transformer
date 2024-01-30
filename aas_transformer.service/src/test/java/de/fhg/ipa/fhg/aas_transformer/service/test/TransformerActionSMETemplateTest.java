package de.fhg.ipa.fhg.aas_transformer.service.test;

import de.fhg.ipa.aas_transformer.model.*;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.util.ArrayList;

import static de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig.AAS;
import static de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig.getAnsibleFactsSubmodel;
import static org.assertj.core.api.Assertions.assertThat;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class TransformerActionSMETemplateTest extends AbstractIT {

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
        this.assertSubmodelExists( getAnsibleFactsSubmodel().getIdShort());

        // Init testTransformer
        String idShort = "sm_template_test";
        CustomId customId = new CustomId(idShort);

        this.testTransformer = new Transformer(new Destination(new DestinationSM(idShort, customId)));
        SubmodelIdentifier sourceSubmodelIdentifier = new SubmodelIdentifier(
                SubmodelIdentifierType.ID_SHORT,
                "ansible-facts"
        );
        ArrayList<TransformerAction> transformerActions = new ArrayList<>() {{
            add(new TransformerActionSubmodelElementTemplate(
                    sourceSubmodelIdentifier,
                    TransformerActionSMETemplateTestConfig.SUBMODEL_ELEMENT_TEMPLATE));
        }};
        this.testTransformer.addTransformerActions(transformerActions);
    }

    @Test
    @Order(10)
    public void executeTransformer() {
        var transformerService = transformerServiceFactory.create(testTransformer);
        transformerService.execute(GenericTestConfig.AAS.getIdentification(), null);

        this.assertSubmodelExists("sm_template_test");

        var destinationSubmodel = aasManager.retrieveSubmodel(
                AAS.getIdentification(),
                testTransformer.getDestination().getSmDestination().getIdentifier()
        );
        var destinationSubmodelElements = destinationSubmodel.getSubmodelElements();

        assertThat(destinationSubmodelElements).containsKeys("distribution_templated");
        assertThat(destinationSubmodelElements.get("distribution_templated").getValue()).isEqualTo("Ubuntu");
    }

}
