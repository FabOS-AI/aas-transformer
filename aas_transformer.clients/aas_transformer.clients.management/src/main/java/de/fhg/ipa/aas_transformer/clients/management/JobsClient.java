package de.fhg.ipa.aas_transformer.clients.management;

import de.fhg.ipa.aas_transformer.clients.ApiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JobsClient extends JobMetricsRestControllerApi {
    private static final Logger LOG = LoggerFactory.getLogger(JobsClient.class);

    public JobsClient(@Value("${aas_transformer.services.management.base-url}") String baseUrl) {
        super(new ApiClient().setBasePath(baseUrl));
        LOG.info("Initializing JobsClient with base URL: {}", baseUrl);
    }
}
