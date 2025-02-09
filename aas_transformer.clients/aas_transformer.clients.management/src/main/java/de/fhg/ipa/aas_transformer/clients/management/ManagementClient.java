package de.fhg.ipa.aas_transformer.clients.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.fhg.ipa.aas_transformer.clients.ApiClient;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ManagementClient extends TransformerRestControllerApi {
    private static ObjectMapper objectMapper;
    static {
        objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(new SubmodelSerializer(Submodel.class));
        objectMapper.registerModule(simpleModule);
    }

    public ManagementClient(@Value("${aas_transformer.services.management.base-url}") String baseUrl) {
        super(new ApiClient(
                objectMapper,
                ApiClient.createDefaultDateFormat()
            ).setBasePath(baseUrl))
        ;
    }
}
