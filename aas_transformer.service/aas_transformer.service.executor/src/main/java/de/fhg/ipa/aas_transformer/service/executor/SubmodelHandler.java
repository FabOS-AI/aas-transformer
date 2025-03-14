package de.fhg.ipa.aas_transformer.service.executor;

import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.basyx.submodelservice.pathparsing.HierarchicalSubmodelElementParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class SubmodelHandler {
    @Autowired
    Executor executor;

    public SubmodelElement getSubmodelElement(
            String b64SubmodelIdentifier,
            String b64SubmodelElementIdentifier
    ) {
        String submodelId = new String(Base64.getDecoder().decode(b64SubmodelIdentifier));
        String submodelElementId = new String(Base64.getDecoder().decode(b64SubmodelElementIdentifier));
        Submodel submodel = executor.executeOnRequest(submodelId);
        HierarchicalSubmodelElementParser parser = new HierarchicalSubmodelElementParser(submodel);
        return parser.getSubmodelElementFromIdShortPath(submodelElementId);
    }
}
