package de.fhg.ipa.fhg.aas_transformer.service.test.instances;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.fhg.aas_transformer.service.test.AbstractIT;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static de.fhg.ipa.fhg.aas_transformer.service.test.GenericTestConfig.objectMapper;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

public class InstanceTest extends AbstractIT {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceTest.class);

    protected String sourceSubmodelFilename;
    protected Submodel sourceSubmodel;
    protected final String transformerFilename;
    protected Transformer transformer;

    public InstanceTest(String sourceSubmodelFilename, String transformerFilename) {
        this.sourceSubmodelFilename = sourceSubmodelFilename;
        this.transformerFilename = transformerFilename;
    }

    @BeforeAll
    public void beforeAll() throws IOException, DeserializationException {
        super.beforeAll();

        var sourceSubmodelFile = new File("src/test/resources/submodels/"  + sourceSubmodelFilename);
        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        this.sourceSubmodel = jsonDeserializer.read(new FileInputStream(sourceSubmodelFile), Submodel.class);
        this.submodelRepository.createOrUpdateSubmodel(sourceSubmodel);

        var transformerFile = new File("src/test/resources/transformer/" + transformerFilename);
        this.transformer = objectMapper.readValue(transformerFile, Transformer.class);
    }

    protected void assertSubmodelExists(String submodelId) {
        assertThatCode(() -> {
            var submodel = this.submodelRepository.getSubmodel(submodelId);
            assertThat(submodel).isNotNull();
        }).doesNotThrowAnyException();
    }
}
