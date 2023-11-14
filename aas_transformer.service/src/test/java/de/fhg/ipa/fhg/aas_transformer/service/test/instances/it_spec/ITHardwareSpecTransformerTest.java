package de.fhg.ipa.fhg.aas_transformer.service.test.instances.it_spec;

import de.fhg.ipa.aas_transformer.model.MqttVerb;
import de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig;
import de.fhg.ipa.fhg.aas_transformer.service.test.instances.InstanceTest;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import static de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig.AAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ITHardwareSpecTransformerTest extends InstanceTest {
    public ITHardwareSpecTransformerTest() {
        super("ansible-facts", "it_hardware_spec");
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
        var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());
        assertThat(submodels).containsKey("it_hardware_spec");
    }

    @Test
    @Order(30)
    public void checkSubmodelElementCountEqualsTransformerActionCount() {
        ISubmodel newSubmodel = aasManager.retrieveSubmodel(
                GenericTestConfig.AAS.getIdentification(),
                transformer.getDestination().getSmDestination().getIdentifier()
        );

        assertEquals(
                transformer.getTransformerActions().size(),
                newSubmodel.getSubmodelElements().size()
        );
    }
}
