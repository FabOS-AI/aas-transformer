package de.fhg.ipa.aas_transformer.aas;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiClient;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.eclipse.digitaltwin.basyx.aasregistry.client.api.RegistryAndDiscoveryInterfaceApi;
import org.eclipse.digitaltwin.basyx.aasregistry.client.model.AssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.basyx.aasregistry.client.model.AssetKind;
import org.eclipse.digitaltwin.basyx.aasregistry.client.model.GetAssetAdministrationShellDescriptorsResult;
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
public class AasRegistry {

    private static final Logger LOG = LoggerFactory.getLogger(AasRegistry.class);
    private static final int DEFAULT_PAGE_SIZE = 1000;

    private final String aasRegistryUrl;

    private final String aasRepositoryUrl;

    private RegistryAndDiscoveryInterfaceApi aasRegistryApi;

    public AasRegistry(@Value("${aas.aas-registry.url}") String aasRegistryUrl,
                       @Value("${aas.aas-repository.url}") String aasRepositoryUrl) {
        this.aasRegistryUrl = aasRegistryUrl;
        this.aasRepositoryUrl = aasRepositoryUrl;
        var objectMapper = new ObjectMapper();
        var aasRegistryClient = new ApiClient(HttpClient.newBuilder(), objectMapper, this.aasRegistryUrl);
        this.aasRegistryApi = new RegistryAndDiscoveryInterfaceApi(aasRegistryClient);
        LOG.info("AasRegistry initialized with URL: {}", aasRegistryUrl);
    }

    public Optional<AssetAdministrationShellDescriptor> getAasDescriptor(String aasId) throws ApiException {
        try {
            var result = this.aasRegistryApi.getAssetAdministrationShellDescriptorByIdWithHttpInfo(aasId);
            return Optional.of(result.getData());
        } catch (ApiException e) {
            if (e.getCode() == 404) {
                return Optional.empty();
            } else {
                throw e;
            }
        }
    }

    public void addSubmodelDescriptorToAas(String aasId, SubmodelDescriptor submodelDescriptor) throws ApiException {
        var endpoints = new ArrayList<org.eclipse.digitaltwin.basyx.aasregistry.client.model.Endpoint>();
        var endpoint = new org.eclipse.digitaltwin.basyx.aasregistry.client.model.Endpoint();
        endpoint.setInterface("SUBMODEL-3.0");
        var protocolInformation = new org.eclipse.digitaltwin.basyx.aasregistry.client.model.ProtocolInformation();
        protocolInformation.setEndpointProtocol("http");
        protocolInformation.setHref(submodelDescriptor.getEndpoints().get(0).getProtocolInformation().getHref());
        endpoint.setProtocolInformation(protocolInformation);
        endpoints.add(endpoint);

        var convertedSubmodelDescriptor = new org.eclipse.digitaltwin.basyx.aasregistry.client.model.SubmodelDescriptor();
        convertedSubmodelDescriptor.setId(submodelDescriptor.getId());
        convertedSubmodelDescriptor.setIdShort(submodelDescriptor.getIdShort());
        convertedSubmodelDescriptor.setEndpoints(endpoints);

        this.addSubmodelDescriptorToAas(aasId, convertedSubmodelDescriptor);
    }

    public void addSubmodelDescriptorToAas(String aasId, org.eclipse.digitaltwin.basyx.aasregistry.client.model.SubmodelDescriptor submodelDescriptor) throws ApiException {
        try {
            this.aasRegistryApi.postSubmodelDescriptorThroughSuperpathWithHttpInfo(aasId, submodelDescriptor);
        } catch (ApiException e) {
            if (e.getCode() == 409) {
                this.aasRegistryApi.putSubmodelDescriptorByIdThroughSuperpath(aasId, submodelDescriptor.getId(), submodelDescriptor);
            }
            else {
                throw new RuntimeException(e);
            }
        }
    }

    public void removeSubmodelDescriptorFromAas(String shellId, String submodelId) {
        try {
            this.aasRegistryApi.deleteSubmodelDescriptorByIdThroughSuperpath(shellId, submodelId);
        } catch (ApiException e) {
            LOG.error("Failed to remove submodel descriptor with id = {} shell descriptor with id = {} | {}",
                    submodelId,
                    shellId,
                    e.getMessage());
        }
    }

    public void removeSubmodelDescriptorFromAllAas(String submodelId) {
        try {
            // TODO Check assetKind & assetType
            List<AssetAdministrationShellDescriptor> aasDescriptors = this.aasRegistryApi.getAllAssetAdministrationShellDescriptors(
                    DEFAULT_PAGE_SIZE,
                    "",
                    null,
                    null
            ).getResult();
            for (var aasDescriptor : aasDescriptors) {
                var submodelDescriptors = aasDescriptor.getSubmodelDescriptors();
                for (var submodelDescriptor : submodelDescriptors) {
                    if (submodelDescriptor.getId().equals(submodelId)) {
                        try {
                            this.aasRegistryApi.deleteSubmodelDescriptorByIdThroughSuperpath(aasDescriptor.getId(), submodelId);
                        } catch (ApiException e) {
                            LOG.warn("Could not remove submodel descriptor with id = {} from AAS with id = {} | {} ",
                                    aasDescriptor.getId(),
                                    submodelDescriptor.getId(),
                                    e.getMessage()
                            );
                        }
                    }
                }
            }
        } catch (ApiException e) {
            LOG.error("Could not remove submodel descriptors with id = {} because looking up shell descriptors failed | {}",
                    submodelId,
                    e.getMessage()
            );
        }

    }
}
