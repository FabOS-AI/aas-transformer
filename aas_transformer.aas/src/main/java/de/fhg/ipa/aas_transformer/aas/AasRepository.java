package de.fhg.ipa.aas_transformer.aas;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.digitaltwin.basyx.aasrepository.client.ConnectedAasRepository;
import org.eclipse.digitaltwin.basyx.core.exceptions.CollidingIdentifierException;
import org.eclipse.digitaltwin.basyx.core.exceptions.CollidingSubmodelReferenceException;
import org.eclipse.digitaltwin.basyx.core.pagination.PaginationInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
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
    }

    public void createAasOrDoNothing(AssetAdministrationShell aas) {
        try {
            this.connectedAasRepository.createAas(aas);
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

    public AssetAdministrationShell getAas(String aasId) {
        var aas = this.connectedAasRepository.getAas(aasId);

        return aas;
    }

    public void addSubmodelReferenceToAas(String aasId, Submodel submodel) {
        var submodelReference = new DefaultReference.Builder()
                .keys(new DefaultKey.Builder()
                        .type(KeyTypes.SUBMODEL)
                        .value(submodel.getId()).build())
                .build();
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
