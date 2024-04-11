package de.fhg.ipa.fhg.aas_transformer.service.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;


public class AasFileReaderTest {

    public static File simpleSubmodelFile = new File("src/test/resources/submodels/simple-submodel.json");

    @Test
    public void testParsing() throws IOException, DeserializationException {
        var objectMapper = new ObjectMapper();

        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        var submodel = jsonDeserializer.read(new FileInputStream(simpleSubmodelFile), Submodel.class);

        var submodelRepository = new SubmodelRepository("http://localhost:8081");

        submodelRepository.createOrUpdateSubmodel(submodel);
    }

}
