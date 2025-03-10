package de.fhg.ipa.aas_transformer.service.executor;

import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;
import java.util.List;

@RestController
@RequestMapping("/submodels")
public class SubmodelRepositoryRestController {
    private static final Logger LOG = LoggerFactory.getLogger(SubmodelRepositoryRestController.class);
    @Autowired
    Executor executor;

    @RequestMapping(path = "", method = RequestMethod.GET, produces = "application/json")
    public List<Submodel> getSubmodels() {
        LOG.info("Received request for all submodels.");
        return executor.executeBatchOnRequest();
    }

    @RequestMapping(path = "/{submodelIdentifier}", method = RequestMethod.GET, produces = "application/json")
    public Submodel getSubmodel(
            @PathVariable(name = "submodelIdentifier") String submodelIdentifier
    ) {
        String decodedSubmodelId = new String(Base64.getDecoder().decode(submodelIdentifier));
        LOG.info("Received request for submodel with id: " + decodedSubmodelId);
        return executor.executeOnRequest(decodedSubmodelId);
    }

    @RequestMapping(path = "/{submodelIdentifier}/submodel-elements/{submodelElementIdentifier}", method = RequestMethod.GET, produces = "application/json")
    public SubmodelElement getSubmodelElement(
            @PathVariable(name = "submodelIdentifier") String submodelIdentifier,
            @PathVariable(name = "submodelElementIdentifier") String submodelElementIdentifier
    ) {
        return null;
    }
}
