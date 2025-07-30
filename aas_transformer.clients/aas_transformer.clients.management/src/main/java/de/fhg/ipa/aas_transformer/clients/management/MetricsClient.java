package de.fhg.ipa.aas_transformer.clients.management;

import de.fhg.ipa.aas_transformer.clients.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class MetricsClient extends MetricsRestControllerApi {

    private static final Logger LOG = LoggerFactory.getLogger(MetricsClient.class);

    public MetricsClient(@Value("${aas_transformer.services.management.base-url}") String baseUrl) {
        super(new ApiClient().setBasePath(baseUrl));
        LOG.info("Initializing MetricsClient with base URL: {}", baseUrl);
    }
}
