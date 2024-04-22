package de.fhg.ipa.aas_transformer.service.aas;

import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.KeyTypes;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.digitaltwin.basyx.aasrepository.client.ConnectedAasRepository;
import org.eclipse.digitaltwin.basyx.core.exceptions.CollidingIdentifierException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AasRepository {

    private static final Logger LOG = LoggerFactory.getLogger(AasRepository.class);

    private final String aasRepositoryUrl;

    private final ConnectedAasRepository connectedAasRepository;

    public AasRepository(@Value("${aas.aas-repository.url}") String aasRepositoryUrl) {
        this.aasRepositoryUrl = aasRepositoryUrl;
        this.connectedAasRepository = new ConnectedAasRepository(this.aasRepositoryUrl);
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
        this.connectedAasRepository.addSubmodelReference(aasId, submodelReference);
    }
}
