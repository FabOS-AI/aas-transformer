package de.fhg.ipa.fhg.aas_transformer.service.test.instances.operating_system;

import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import de.fhg.ipa.fhg.aas_transformer.service.test.instances.InstanceTest;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OperatingSystemTransformerTest extends InstanceTest {

    private static final Logger LOG = LoggerFactory.getLogger(OperatingSystemTransformerTest.class);

    public OperatingSystemTransformerTest() {
        super("ansible-facts-submodel.json", "operating_system.json");
    }

    @Test
    @Order(10)
    public void executeTransformer()  {
        var transformerService = this.transformerServiceFactory.create(this.transformer);
        var submodel = this.submodelRepository.getSubmodel(sourceSubmodel.getId());
        transformerService.execute(submodel);
    }

    @Test
    @Order(20)
    public void checkOperatingSystemSubmodelExists() {
        this.assertSubmodelExists(this.transformer.getDestination().getSubmodelDestination().getId());
    }

    @Test
    @Order(30)
    public void checkSubmodelElementCountEqualsTransformerActionCount() {
        var newSubmodel = submodelRepository.getSubmodel(transformer.getDestination().getSubmodelDestination().getId());

        for (var submodelElement : newSubmodel.getSubmodelElements()) {
            LOG.warn("Id: " + submodelElement.getIdShort() + " | Value: " + ((Property)submodelElement).getValue());
        }

        for(TransformerAction a : transformer.getTransformerActions()) {
            var copyAction = (TransformerActionCopy) a;
            var sourceSubmodelElementKey = copyAction.getSourceSubmodelElement();

            var sourceSubmodelElement = sourceSubmodel.getSubmodelElements().stream()
                    .filter(sme -> sme.getIdShort().equals(sourceSubmodelElementKey)).findAny().get();

            assertThat(newSubmodel.getSubmodelElements())
                    .extracting(SubmodelElement::getIdShort)
                    .contains(sourceSubmodelElementKey);

            var destinationSubmodelElement = newSubmodel.getSubmodelElements().stream()
                    .filter(sme -> sme.getIdShort().equals(sourceSubmodelElementKey)).findAny().get();
            assertThat(destinationSubmodelElement)
                    .usingRecursiveComparison()
                    .comparingOnlyFields("value", "idShort")
                    .isEqualTo(sourceSubmodelElement);;
        }
    }
}
