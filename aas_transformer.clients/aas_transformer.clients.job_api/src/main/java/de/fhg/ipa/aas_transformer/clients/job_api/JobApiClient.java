package de.fhg.ipa.aas_transformer.clients.job_api;

import de.fhg.ipa.aas_transformer.clients.ApiClient;
import de.fhg.ipa.aas_transformer.clients.management.JobRestControllerApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JobApiClient extends JobRestControllerApi {
    public JobApiClient(@Value("${aas_transformer.services.job_api.base-url}") String baseUrl) {
        super(new ApiClient().setBasePath(baseUrl));
    }
}
