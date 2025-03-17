package de.fhg.ipa.aas_transformer.service.executor.controller;

import de.fhg.ipa.aas_transformer.service.executor.SubmodelDescriptorHandler;
import org.eclipse.digitaltwin.basyx.submodelregistry.client.model.SubmodelDescriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@RestController
@RequestMapping("/submodel-descriptors")
@CrossOrigin(origins = "*")
public class SubmodelRegistryRestController {
    private static final Logger LOG = LoggerFactory.getLogger(SubmodelRegistryRestController.class);

    @Autowired
    SubmodelDescriptorHandler submodelDescriptorHandler;

    @RequestMapping(path = "", method = RequestMethod.GET, produces = APPLICATION_JSON)
    public List<SubmodelDescriptor> getSubmodelDescriptors() {
        LOG.info("Received request for all submodels descriptors.");
        return submodelDescriptorHandler.getSubmodelDescriptors();
    }

    @RequestMapping(path = "/{submodelIdentifier}", method = RequestMethod.GET, produces = APPLICATION_JSON)
    public SubmodelDescriptor getSubmodelDescriptor(@PathVariable(name = "submodelIdentifier") String submodelIdentifier) {
        LOG.info("Received request for submodels descriptor with id = {}.", submodelIdentifier);
        return submodelDescriptorHandler.getSubmodelDescriptor(submodelIdentifier);
    }
}
