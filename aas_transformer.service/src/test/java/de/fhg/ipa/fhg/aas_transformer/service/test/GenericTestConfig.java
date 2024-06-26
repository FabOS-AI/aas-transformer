package de.fhg.ipa.fhg.aas_transformer.service.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class GenericTestConfig {
    public static int SUBMODEL_REGISTRY_PUBLIC_PORT = 8083;

    public static SubmodelRegistry submodelRegistry;

    public static int SUBMODEL_REPOSITORY_PUBLIC_PORT = 8081;

    public static ConnectedSubmodelRepository submodelRepository;
    public static ObjectMapper objectMapper = new ObjectMapper();

    public static File simpleSubmodelFile = new File("src/test/resources/submodels/simple-submodel.json");

    public static String getAASRegistryUrl() {
        return "http://localhost:"+ SUBMODEL_REGISTRY_PUBLIC_PORT +"/registry";
    }
    public static String getAASServerUrl() {
        return "http://localhost:"+ SUBMODEL_REPOSITORY_PUBLIC_PORT +"/aasServer";
    }

    public static Submodel getSimpleSubmodel() throws IOException, DeserializationException {
        JsonDeserializer jsonDeserializer = new JsonDeserializer();
        var submodel = jsonDeserializer.read(new FileInputStream(simpleSubmodelFile), Submodel.class);

        return submodel;
    }
}
