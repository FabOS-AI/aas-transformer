package de.fhg.ipa.aas_transformer.aas;

import com.fasterxml.jackson.databind.JsonNode;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.basyx.core.exceptions.CollidingIdentifierException;
import org.eclipse.digitaltwin.basyx.core.exceptions.ElementDoesNotExistException;
import org.eclipse.digitaltwin.basyx.core.pagination.CursorResult;
import org.eclipse.digitaltwin.basyx.core.pagination.PaginationInfo;
import org.eclipse.digitaltwin.basyx.submodelrepository.client.ConnectedSubmodelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

@Component
public class SubmodelRepository {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelRepository.class);

    private final static int DEFAULT_IN_MEMORY_SIZE = 16 * 1024 * 1024;
    private final String submodelRepositoryUrl;

    private final ConnectedSubmodelRepository connectedSubmodelRepository;

    public SubmodelRepository(
            @Value("${aas.submodel-repository.url}") String submodelRepositoryUrl
    ) {
        this.submodelRepositoryUrl = submodelRepositoryUrl;
        this.connectedSubmodelRepository = new ConnectedSubmodelRepository(submodelRepositoryUrl);
    }

    public List<Submodel> getAllSubmodels() throws DeserializationException {

        int limit = 100;
        String cursor = "";
        List<Submodel> submodels = new ArrayList<>();
        do {
            CursorResult<List<Submodel>> resultSms = this.connectedSubmodelRepository.getAllSubmodels(
                    new PaginationInfo(limit, cursor)
            );
            submodels.addAll(resultSms.getResult());
            cursor = resultSms.getCursor();
        } while(cursor != null);


        return submodels;
    }

    public Submodel getSubmodel(String submodelId) {
        var submodel = this.connectedSubmodelRepository.getSubmodel(submodelId);
        return submodel;
    }

    public void createOrUpdateSubmodel(Submodel submodel) {
        try {
            this.connectedSubmodelRepository.createSubmodel(submodel);
        } catch (CollidingIdentifierException e) {
            this.connectedSubmodelRepository.updateSubmodel(submodel.getId(), submodel);
        }
        catch (RuntimeException e) {
            LOG.error(e.getMessage());
        }
    }

    public void deleteSubmodel(String submodelId) {
        this.connectedSubmodelRepository.deleteSubmodel(submodelId);
    }

    public void deleteAllSubmodels() throws DeserializationException {
        getAllSubmodels().forEach(submodel -> {
            deleteSubmodel(submodel.getId());
        });
    }

    public SubmodelElement getSubmodelElement(String submodelId, String smeIdShort) {
        var submodelElement = this.connectedSubmodelRepository.getSubmodelElement(submodelId, smeIdShort);
        return submodelElement;
    }

    public void createSubmodelElement(String submodelId, SubmodelElement submodelElement) {
        this.connectedSubmodelRepository.createSubmodelElement(submodelId, submodelElement);
    }

    public void updateSubmodelElement(String submodelId, String idShortPath, SubmodelElement submodelElement) {
        this.connectedSubmodelRepository.updateSubmodelElement(submodelId, idShortPath, submodelElement);
    }

    public void createOrUpdateSubmodelElement(String submodelId, String idShortPath, SubmodelElement submodelElement) {
        try {
            this.updateSubmodelElement(submodelId, idShortPath, submodelElement);
        } catch (ElementDoesNotExistException e) {
            submodelElement.setIdShort(idShortPath);
            this.connectedSubmodelRepository.createSubmodelElement(
                    submodelId,
                    submodelElement
            );
        }
    }

}
