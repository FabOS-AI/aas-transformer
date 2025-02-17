package de.fhg.ipa.aas_transformer.clients.management;

import de.fhg.ipa.aas_transformer.clients.ApiClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ScalingClient extends TransformerRestControllerApi  {
    public ScalingClient(@Value("${aas_transformer.services.management.base-url}") String baseUrl) {
        super(new ApiClient().setBasePath(baseUrl));
    }
}
