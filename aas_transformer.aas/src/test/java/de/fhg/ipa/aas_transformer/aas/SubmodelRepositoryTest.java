package de.fhg.ipa.aas_transformer.aas;


import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.junit.jupiter.api.*;

@Disabled
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class SubmodelRepositoryTest {

    @Order(10)
    @Test
    public void testGetExtSubmodel() {
        SubmodelRegistry submodelRegistry = new SubmodelRegistry("http://eol-test.h2/sm-registry","http://submodel-repo.local");

        Submodel submodel = SubmodelRepository.getExtSubmodel(submodelRegistry, "Lot_660_lotdata");
    }
}
