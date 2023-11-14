package de.fhg.ipa.fhg.aas_transformer.service.test;

import de.fhg.ipa.aas_transformer.model.*;
import org.assertj.core.groups.Tuple;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.stream.Collectors;

import static de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig.AAS;
import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InitialTests extends AbstractIT {
    public final static Logger LOG = LoggerFactory.getLogger(InitialTests.class);

    @Test
    @Order(10)
    public void testAASCreation() throws IOException {
        this.aasManager.createAAS(GenericTestConfig.AAS);

        var aasCollection = aasManager.retrieveAASAll();

        assertThat(aasCollection)
                .extracting("idShort", "identification")
                .contains(new Tuple(GenericTestConfig.AAS.getIdShort(), GenericTestConfig.AAS.getIdentification()));
    }

    @Test
    @Order(20)
    public void testSubmodelCreation() throws IOException {
        this.aasManager.createSubmodel(
                GenericTestConfig.AAS.getIdentification(),
                GenericTestConfig.getAnsibleFactsSubmodel()
        );

        var aasSubmodelCollection = this.aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());
        assertThat(aasSubmodelCollection).containsKey(GenericTestConfig.getAnsibleFactsSubmodel().getIdShort());
    }

    @Test
    @Order(30)
    public void testTransformerActionCopy() {
        String idShort = "operating_system";
        CustomId customId = new CustomId(idShort);

        Transformer transformer = new Transformer(new Destination(new DestinationSM(idShort, customId)));
        SubmodelIdentifier sourceSubmodelIdentifier = new SubmodelIdentifier(
                SubmodelIdentifierType.ID_SHORT,
                "ansible-facts"
        );
        var transformerActions = new ArrayList<TransformerActionCopy>() {{
            add(new TransformerActionCopy(sourceSubmodelIdentifier,"distribution"));
            add(new TransformerActionCopy(sourceSubmodelIdentifier,"distribution_major_version"));
        }};

        transformer.addTransformerActions(transformerActions);
        var transformerService = this.transformerServiceFactory.create(transformer);
        transformerService.execute(AAS.getIdentification());

        var newSubmodel = aasManager.retrieveSubmodel(AAS.getIdentification(), customId);
        assertThat(newSubmodel).isNotNull();

        var expectedSubmodelElementIds = transformerActions.stream()
                .map(TransformerActionCopy::getSourceSubmodelElement)
                .collect(Collectors.toList());
        assertThat(newSubmodel.getSubmodelElements().keySet()).containsAll(expectedSubmodelElementIds);
    }
}
