package de.fhg.ipa.aas_transformer.clients.management;

import de.fhg.ipa.aas_transformer.clients.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ScalingClient extends ScalingRestControllerApi  {
    private static final Logger LOG = LoggerFactory.getLogger(ScalingClient.class);
    public ScalingClient(@Value("${aas_transformer.services.management.base-url}") String baseUrl) {
        super(new ApiClient().setBasePath(baseUrl));
        LOG.info("Initializing ScalingClient with base URL: {}", baseUrl);
    }
}
