package de.fhg.ipa.aas_transformer.service.executor;

import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.basyx.submodelservice.pathparsing.HierarchicalSubmodelElementParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;
import java.util.List;

@Component
public class SubmodelHandler {
    @Autowired
    Executor executor;

    public List<Submodel> getSubmodels() {
        return executor.executeBatchOnRequest();
    }

    public Submodel getSubmodel(String submodelIdentifier) {
//        String submodelId = new String(Base64.getDecoder().decode(submodelIdentifier));
        return executor.executeOnRequest(submodelIdentifier);
    }

    public List<SubmodelElement> getSubmodelElements(String submodelIdentifier) {
        return getSubmodel(submodelIdentifier).getSubmodelElements();
    }

    public SubmodelElement getSubmodelElement(
            String submodelIdentifier,
            String submodelElementIdentifier
    ) {
//        String submodelId = new String(Base64.getDecoder().decode(submodelIdentifier));
//        String submodelElementId = new String(Base64.getDecoder().decode(submodelElementIdentifier));
        Submodel submodel = getSubmodel(submodelIdentifier);
        HierarchicalSubmodelElementParser parser = new HierarchicalSubmodelElementParser(submodel);
        return parser.getSubmodelElementFromIdShortPath(submodelElementIdentifier);
    }
}
