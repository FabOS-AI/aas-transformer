package de.fhg.ipa.aas_transformer.aas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.http.Base64UrlEncodedIdentifier;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.ApiException;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.api.SubmodelRegistryApi;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.model.Endpoint;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.model.SubmodelDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.Optional;

@Component
public class SubmodelRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelRegistry.class);

    private final String submodelRegistryUrl;

    private final String submodelRepositoryUrl;

    private SubmodelRegistryApi submodelRegistryApi;

    public SubmodelRegistry(@Value("${aas.submodel-registry.url}") String submodelRegistryUrl,
                            @Value("${aas.submodel-repository.url}") String submodelRepositoryUrl) {
        this.submodelRegistryUrl = submodelRegistryUrl;
        this.submodelRepositoryUrl = submodelRepositoryUrl;
        var objectMapper = new ObjectMapper();
        var submodelRegistryClient = new org.eclipse.digitaltwin.basyx.submodelregistry.client.ApiClient(HttpClient.newBuilder(), objectMapper, this.submodelRegistryUrl);
        this.submodelRegistryApi = new SubmodelRegistryApi(submodelRegistryClient);
        LOG.info("SubmodelRegistry initialized with URL: {}", submodelRegistryUrl);
    }

    public Optional<SubmodelDescriptor> findSubmodelDescriptor(String submodelId) {
        SubmodelDescriptor submodelDescriptor = null;
        try {
            submodelDescriptor = this.submodelRegistryApi.getSubmodelDescriptorById(submodelId);
            return Optional.of(submodelDescriptor);
        } catch (ApiException e) {
            if (e.getCode() != 404) {
                LOG.error(e.getMessage());
            }
            return Optional.empty();
        }
    }

    public void registerSubmodel(Submodel submodel) throws ApiException {
        var endpoints = new ArrayList<Endpoint>();
        var endpoint = new Endpoint();
        endpoint.setInterface("SUBMODEL-3.0");
        var protocolInformation = new org.eclipse.digitaltwin.basyx.submodelregistry.client.model.ProtocolInformation();
        protocolInformation.setEndpointProtocol("http");
        protocolInformation.setHref(this.submodelRepositoryUrl + "/submodels/" + Base64UrlEncodedIdentifier.encodeIdentifier(submodel.getId()));
        endpoint.setProtocolInformation(protocolInformation);
        endpoints.add(endpoint);

        var submodelDescriptor = new SubmodelDescriptor();
        submodelDescriptor.setId(submodel.getId());
        submodelDescriptor.setIdShort(submodel.getIdShort());
        submodelDescriptor.setEndpoints(endpoints);

        try {
            this.submodelRegistryApi.postSubmodelDescriptor(submodelDescriptor);
        } catch (ApiException e) {
            if (e.getCode() == 409) {
                this.submodelRegistryApi.putSubmodelDescriptorById(submodelDescriptor.getId(), submodelDescriptor);
            }
            else {
                throw e;
            }
        }
    }
}
