package de.fhg.ipa.aas_transformer.service.executor.controller;

import de.fhg.ipa.aas_transformer.service.executor.SubmodelHandler;
import jakarta.ws.rs.Produces;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RestController
@RequestMapping("/submodels")
@CrossOrigin(origins = "*")
@Produces(APPLICATION_JSON)
public class SubmodelRepositoryRestController {
    private static final Logger LOG = LoggerFactory.getLogger(SubmodelRepositoryRestController.class);
    @Autowired
    SubmodelHandler submodelHandler;

    @RequestMapping(path = "", method = RequestMethod.GET, produces = APPLICATION_JSON)
    public List<Submodel> getSubmodels() {
        LOG.info("Received request for all submodels.");
        return submodelHandler.getSubmodels();
    }

    @RequestMapping(path = "/{submodelIdentifier}", method = RequestMethod.GET, produces = APPLICATION_JSON)
    public Submodel getSubmodel(
            @PathVariable(name = "submodelIdentifier") String submodelIdentifier
    ) {
        LOG.info("Received request for submodel with id: {}", submodelIdentifier);
        return submodelHandler.getSubmodel(submodelIdentifier);
    }

    @RequestMapping(path = "/{submodelIdentifier}/submodel-elements", method = RequestMethod.GET, produces = APPLICATION_JSON)
    public List<SubmodelElement> getSubmodelElements(
            @PathVariable(name = "submodelIdentifier") String submodelIdentifier
    ) {
        LOG.info("Received request for submodel elements of submodel with id: {}", submodelIdentifier);
        return submodelHandler.getSubmodelElements(submodelIdentifier);
    }

    @RequestMapping(path = "/{submodelIdentifier}/submodel-elements/{submodelElementIdentifier}", method = RequestMethod.GET, produces = APPLICATION_JSON)
    public SubmodelElement getSubmodelElement(
            @PathVariable(name = "submodelIdentifier") String submodelIdentifier,
            @PathVariable(name = "submodelElementIdentifier") String submodelElementIdentifier
    ) {
        LOG.info("Received request for submodel element with id: {} of submodel with id: {}", submodelElementIdentifier, submodelIdentifier);
        return submodelHandler.getSubmodelElement(submodelIdentifier, submodelElementIdentifier);
    }
}
