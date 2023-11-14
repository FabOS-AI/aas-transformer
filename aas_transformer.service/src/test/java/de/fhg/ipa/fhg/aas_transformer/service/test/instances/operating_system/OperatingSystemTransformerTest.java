package de.fhg.ipa.fhg.aas_transformer.service.test.instances.operating_system;

import de.fhg.ipa.aas_transformer.model.MqttVerb;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig;
import de.fhg.ipa.fhg.aas_transformer.service.test.instances.InstanceTest;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig.AAS;
import static de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig.getAnsibleFactsSubmodel;
import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OperatingSystemTransformerTest extends InstanceTest {

    private static final Logger LOG = LoggerFactory.getLogger(OperatingSystemTransformerTest.class);

    public OperatingSystemTransformerTest() {
        super("ansible-facts", "operating_system");
    }

    @Test
    @Order(10)
    public void executeTransformer()  {
        var transformerService = this.transformerServiceFactory.create(transformer);
        transformerService.execute(GenericTestConfig.AAS.getIdentification());
    }

    @Test
    @Order(20)
    public void checkSubmodelExists() {
        var submodels = aasManager.retrieveSubmodels(AAS.getIdentification());
        assertThat(submodels).containsKey("operating_system");
    }

    @Test
    @Order(30)
    public void checkSubmodelElementCountEqualsTransformerActionCount() throws IOException {
        var newSubmodel = aasManager.retrieveSubmodel(
                AAS.getIdentification(),
                transformer.getDestination().getSmDestination().getIdentifier()
        );

        for (var e : newSubmodel.getSubmodelElements().entrySet()) {
            LOG.warn("Key: " + e.getKey() + " | Value: " + e.getValue());
        }

        for(TransformerAction a : transformer.getTransformerActions()) {
            TransformerActionCopy copyAction = (TransformerActionCopy) a;
            String sourceSubmodelElementKey = copyAction.getSourceSubmodelElement();

            var sourceSubmodelElement = aasManager
                    .retrieveSubmodel(AAS.getIdentification(), getAnsibleFactsSubmodel().getIdentification())
                    .getSubmodelElement(sourceSubmodelElementKey);

            assertThat(newSubmodel.getSubmodelElements())
                    .containsKey(sourceSubmodelElementKey)
                    .usingRecursiveComparison();

            assertThat(newSubmodel.getSubmodelElements().get(sourceSubmodelElementKey))
                    .usingRecursiveComparison()
                    .comparingOnlyFields("value", "idShort")
                    .isEqualTo(sourceSubmodelElement);;
        }
    }
}
