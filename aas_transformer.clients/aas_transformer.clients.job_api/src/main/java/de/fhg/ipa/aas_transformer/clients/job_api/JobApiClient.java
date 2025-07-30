package de.fhg.ipa.aas_transformer.clients.job_api;

import de.fhg.ipa.aas_transformer.clients.ApiClient;
import de.fhg.ipa.aas_transformer.clients.management.JobRestControllerApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JobApiClient extends JobRestControllerApi {
    private static final Logger LOG = LoggerFactory.getLogger(JobApiClient.class);

    public JobApiClient(@Value("${aas_transformer.services.job_api.base-url}") String baseUrl) {
        super(new ApiClient().setBasePath(baseUrl));
        LOG.info("Initializing JobApiClient with base URL: {}", baseUrl);
    }
}
