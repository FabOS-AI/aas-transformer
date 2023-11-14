package de.fhg.ipa.fhg.aas_transformer.service.test.instances;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.fhg.aas_transformer.service.test.AbstractIT;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;

import static de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig.AAS;
import static de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig.objectMapper;
import static io.restassured.RestAssured.given;

public class InstanceTest extends AbstractIT {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceTest.class);

    protected String sourceSubmodelFilename;
    protected final String transformerFilename;
    protected Transformer transformer;

    public InstanceTest(String sourceSubmodelFilename, String transformerFilename) {
        this.sourceSubmodelFilename = sourceSubmodelFilename;
        this.transformerFilename = transformerFilename;
    }

    @BeforeAll
    public void beforeAll() throws IOException {
        super.beforeAll();

        File sourceSubmodelFile = new File(
                "src/test/resources/source_submodels/"  + sourceSubmodelFilename + ".json"
        );

        File transformerFile = new File(
                "src/test/resources/transformer/" + transformerFilename + ".json"
        );

        aasManager.createAAS(AAS);
        String smIdShort =  objectMapper.readTree(sourceSubmodelFile).get("idShort").asText();
        given()
            .contentType(ContentType.JSON)
            .body(sourceSubmodelFile)
            .log()
            .all()
            .put(getAASServerUrl()+"/shells/"+AAS.getIdentification().getId()+"/aas/submodels/"+smIdShort)
            .then()
            .log()
            .all();

        this.transformer = objectMapper.readValue(transformerFile, Transformer.class);
    }
}
