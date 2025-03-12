package de.fhg.ipa.aas_transformer.aas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.basyx.http.Base64UrlEncodedIdentifier;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.ApiException;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.api.SubmodelRegistryApi;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.model.Endpoint;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.model.GetSubmodelDescriptorsResult;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.model.SubmodelDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.util.ArrayList;
import java.util.List;
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

    public List<SubmodelDescriptor> getSubmodelDescriptors() {
        int limit = 100;
        String cursor = "";
        List<SubmodelDescriptor> submodelDescriptors = new ArrayList<>();
        do {
            GetSubmodelDescriptorsResult resultSmds = null;
            try {
                resultSmds = this.submodelRegistryApi.getAllSubmodelDescriptors(
                        limit, cursor
                );
            } catch (ApiException e) {
                throw new RuntimeException(e);
            }
            submodelDescriptors.addAll(resultSmds.getResult());
            cursor = resultSmds.getPagingMetadata().getCursor();
        } while(cursor != null);

        return  submodelDescriptors;
    }

    public Optional<SubmodelDescriptor> findSubmodelDescriptor(String submodelId) {
        SubmodelDescriptor submodelDescriptor = null;
        try {
            submodelDescriptor = this.submodelRegistryApi.getSubmodelDescriptorById(submodelId);
            return Optional.of(submodelDescriptor);
        } catch (ApiException e) {
            LOG.error(e.getMessage());
            return Optional.empty();
        }
    }

    public void registerSubmodelDescriptor(SubmodelDescriptor submodelDescriptor) {
        try {
            this.submodelRegistryApi.postSubmodelDescriptor(submodelDescriptor);
        } catch (ApiException e) {
            if (e.getCode() == 409) {
                try {
                    this.submodelRegistryApi.putSubmodelDescriptorById(submodelDescriptor.getId(), submodelDescriptor);
                } catch (ApiException ex) {
                    throw new RuntimeException(ex);
                }
            }
            else {
                LOG.error(e.getMessage());
            }
        }
    }

    public void registerSubmodel(Submodel submodel) throws ApiException {
        var submodelDescriptor = createSubmodelDescriptor(
                submodel.getId(),
                submodel.getIdShort(),
                this.submodelRepositoryUrl
        );

       registerSubmodelDescriptor(submodelDescriptor);
    }

    public SubmodelDescriptor createSubmodelDescriptor(String submodelId, String submodelIdShort, String smRepoBaseUrl) {
        var endpoints = new ArrayList<Endpoint>();
        var endpoint = new Endpoint();
        endpoint.setInterface("SUBMODEL-3.0");
        var protocolInformation = new org.eclipse.digitaltwin.basyx.submodelregistry.client.model.ProtocolInformation();
        protocolInformation.setEndpointProtocol("http");
        protocolInformation.setHref(smRepoBaseUrl + "/submodels/" + Base64UrlEncodedIdentifier.encodeIdentifier(submodelId));
        endpoint.setProtocolInformation(protocolInformation);
        endpoints.add(endpoint);

        var submodelDescriptor = new SubmodelDescriptor();
        submodelDescriptor.setId(submodelId);
        submodelDescriptor.setIdShort(submodelIdShort);
        submodelDescriptor.setEndpoints(endpoints);
        return submodelDescriptor;
    }

    public void deleteSubmodelDescriptor(String submodelId) {
        try {
            this.submodelRegistryApi.deleteSubmodelDescriptorById(submodelId);
            LOG.info("Deleted submodel descriptor with ID: {}", submodelId);
        } catch (ApiException e) {
            LOG.error("Failed to delete submodel descriptor with id: " + submodelId);
        }
    }
}
