package de.fhg.ipa.aas_transformer.aas;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.ReferenceTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;
import org.eclipse.digitaltwin.basyx.aasregistry.client.model.AssetAdministrationShellDescriptor;
import org.eclipse.digitaltwin.basyx.aasrepository.client.ConnectedAasRepository;
import org.eclipse.digitaltwin.basyx.core.exceptions.CollidingIdentifierException;
import org.eclipse.digitaltwin.basyx.core.exceptions.CollidingSubmodelReferenceException;
import org.eclipse.digitaltwin.basyx.core.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Component
public class AasRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AasRepository.class);
    private static final int DEFAULT_PAGE_SIZE = 1000;

    private final String aasRepositoryUrl;

    private final ConnectedAasRepository connectedAasRepository;

    public AasRepository(@Value("${aas.aas-repository.url}") String aasRepositoryUrl) {
        this.aasRepositoryUrl = aasRepositoryUrl;
        this.connectedAasRepository = new ConnectedAasRepository(this.aasRepositoryUrl);
        LOG.info("AasRepository initialized with URL: {}", aasRepositoryUrl);
    }

    public void createAasOrDoNothing(AssetAdministrationShell aas) {
        try {
            this.connectedAasRepository.createAas(aas);
            LOG.info("Created AAS with ID {}", aas.getId());
        }
        catch (CollidingIdentifierException e) {}
        catch (RuntimeException e) {
            LOG.error(e.getMessage());
        }

    }

    public void createOrUpdateAas(AssetAdministrationShell aas) {
        try {
            this.connectedAasRepository.createAas(aas);
        } catch (CollidingIdentifierException e) {
            this.connectedAasRepository.updateAas(aas.getId(), aas);
        }
        catch (RuntimeException e) {
            LOG.error(e.getMessage());
        }
    }

    public List<AssetAdministrationShell> getAllAas() {
        return this.connectedAasRepository.getAllAas(
                new PaginationInfo(DEFAULT_PAGE_SIZE, null)
        ).getResult();
    }

    public List<AssetAdministrationShell> getAllAasContainingSubmodelBySubmodelId(String submodelId) {
        return getAllAas()
                .stream()
                .filter(aas -> aas
                        .getSubmodels()
                        .stream()
                        .filter(sm -> sm
                                .getKeys()
                                .stream()
                                .filter(k -> k
                                        .getValue()
                                        .equals(submodelId)
                                )
                                .findFirst()
                                .isPresent()
                        )
                        .findFirst()
                        .isPresent()
                )
                .collect(Collectors.toList());
    }

    public static String getEndpointOfExtAas(
            AasRegistry aasRegistry,
            String aasId
    ) {
        try {
            LOG.info("Getting AAS Descriptor with ID {} from AAS registry", aasId);
            AssetAdministrationShellDescriptor descriptor = aasRegistry.getAasDescriptor(aasId).get();
            return descriptor.getEndpoints().get(0).getProtocolInformation().getHref();
        } catch (ApiException e) {
            LOG.error("AAS with ID {} not found in AAS registry", aasId);
            return null;
        } catch (NoSuchElementException e) {
            LOG.error("AAS with ID {} not found in AAS registry", aasId);
            return null;
        }
    }

    public static AssetAdministrationShell getExtAas(
            AasRegistry aasRegistry,
            String aasId
    ) {
        try {
            String endpoint = getEndpointOfExtAas(aasRegistry, aasId);
            LOG.info("Getting shell with ID {} from {}", aasId, endpoint);
            return getExtAas(getAasRepositoryBaseUrl(endpoint), aasId);
        } catch (NoSuchElementException e) {
            LOG.error("AAS with ID {} not found in AAS registry", aasId);
            return null;
        }
    }

    public static AssetAdministrationShell getExtAas(String endpoint, String aasId) {
        if(endpoint == null)
            return null;
        ConnectedAasRepository connectedAasRepository = new ConnectedAasRepository(
                getAasRepositoryBaseUrl(endpoint)
        );
        return connectedAasRepository.getAas(aasId);
    }

    private static String getAasRepositoryBaseUrl(String endpoint) {
        if(endpoint == null)
            return null;
        String delimiter = "/shells";
        int index = endpoint.indexOf(delimiter);
        if(index == -1)
            return endpoint;
        return endpoint.substring(0, index);
    }

    public AssetAdministrationShell getAas(String aasId) {
        var aas = this.connectedAasRepository.getAas(aasId);

        return aas;
    }

    public static void addSubmodelReferenceToExtAas(AasRegistry aasRegistry, String aasId, String submodelId) {
        String endpoint = getEndpointOfExtAas(aasRegistry, aasId);
        ConnectedAasRepository repo = new ConnectedAasRepository(getAasRepositoryBaseUrl(endpoint));
        try {
            repo.addSubmodelReference(aasId, createReferenceToSubmodel(submodelId));
        } catch(CollidingSubmodelReferenceException e) {
            LOG.info("Skipping adding submodel-ref with ID {} to AAS with ID {} because it already exists",
                    submodelId,
                    aasId
            );
        }
    }

    public void addSubmodelReferenceToAas(String aasId, Submodel submodel) {
        addSubmodelReferenceToAas(aasId, submodel.getId());
    }

    public static DefaultReference createReferenceToSubmodel(String submodelId) {
        return new DefaultReference.Builder()
                .type(ReferenceTypes.MODEL_REFERENCE)
                .keys(new DefaultKey.Builder()
                        .type(KeyTypes.SUBMODEL)
                        .value(submodelId).build())
                .build();
    }

    public void addSubmodelReferenceToAas(String aasId, String submodelId) {
        var submodelReference = createReferenceToSubmodel(submodelId);
        try {
            this.connectedAasRepository.addSubmodelReference(aasId, submodelReference);
        } catch(CollidingSubmodelReferenceException e) {
            LOG.info("Skipping adding submodel with ID {} to AAS with ID {} because it already exists",
                    submodelReference.getKeys().get(0).getValue(),
                    aasId
            );
        }
    }

    public void deleteAllAas() {
        getAllAas().forEach(aas -> this.connectedAasRepository.deleteAas(aas.getId()));
    }

    public void removeSubmodelReferenceFromAas(String aasId, String submodelId) {
        this.connectedAasRepository.removeSubmodelReference(aasId, submodelId);
    }

    public void removeSubmodelReferenceFromAllAas(String submodelId) {
        this.getAllAasContainingSubmodelBySubmodelId(submodelId).forEach(aas ->
            this.connectedAasRepository.removeSubmodelReference(
                    aas.getId(),
                    submodelId
            )
        );
    }
}
