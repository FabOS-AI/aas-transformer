package de.fhg.ipa.aas_transformer.service.templating;

import com.hubspot.jinjava.lib.fn.InjectedContextFunctionProxy;
import de.fhg.ipa.aas_transformer.service.TransformerHandler;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import jakarta.validation.constraints.Null;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IdentifierType;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.identifier.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class AASTemplateFunctions extends AbstractTemplateFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(AASTemplateFunctions.class);

    public final static String NAMESPACE = "aas";

    public final AASRegistry aasRegistry;

    public final AASManager aasManager;

    public AASTemplateFunctions(AASRegistry aasRegistry, AASManager aasManager) {
        this.aasRegistry = aasRegistry;
        this.aasManager = aasManager;

        try {
            var resolveToSubmodelElementValueMethodRef = AASTemplateFunctions.class.getDeclaredMethod(
                    "resolveToSubmodelElementValue",
                    String.class, String.class, String.class);
            var smeValueTemplateMethod = InjectedContextFunctionProxy.defineProxy(
                    AASTemplateFunctions.NAMESPACE, "sme_value",
                    resolveToSubmodelElementValueMethodRef, this);
            this.templateFunctions.add(smeValueTemplateMethod);

            var resolveToSubmodelElementValueBySemanticIdMethodRef = AASTemplateFunctions.class.getDeclaredMethod(
                    "resolveToSubmodelElementValueBySemanticId",
                    String.class, String.class, String.class);
            var smeValueBySemanticIdTemplateMethod = InjectedContextFunctionProxy.defineProxy(
                    AASTemplateFunctions.NAMESPACE, "sme_value_semantic",
                    resolveToSubmodelElementValueBySemanticIdMethodRef, this);
            this.templateFunctions.add(smeValueBySemanticIdTemplateMethod);

            var findSubmodelByPropertyValueMethodRef = AASTemplateFunctions.class.getDeclaredMethod(
                    "findSubmodelByPropertyValue",
                    String.class, String.class);
            var findSubmodelByPropTemplateMethod = InjectedContextFunctionProxy.defineProxy(
                    AASTemplateFunctions.NAMESPACE, "find_sm_by_prop",
                    findSubmodelByPropertyValueMethodRef, this);
            this.templateFunctions.add(findSubmodelByPropTemplateMethod);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public String resolveToSubmodelElementValue(String aasId, String smId, String smeId) {
        var aasIdentifier = new Identifier(IdentifierType.CUSTOM, aasId);
        var smIdentifier = new Identifier(IdentifierType.CUSTOM, smId);

        try {

            var aas = aasManager.retrieveAAS(aasIdentifier);
            var submodel = aas.getSubmodel(smIdentifier);
            var submodelElement = submodel.getSubmodelElement(smeId);
            var submodelElementLocalCopy = submodelElement.getLocalCopy();
            var value = submodelElementLocalCopy.getValue();

            return value.toString();
        }
        catch (Exception e) {
            LOG.error(e.getMessage());
            throw e;
        }

    }

    public String resolveToSubmodelElementValueBySemanticId(String aasId, String smSemanticId, String smeId) {
        var aasIdentifier = new Identifier(IdentifierType.CUSTOM, aasId);

        var aas = aasManager.retrieveAAS(aasIdentifier);
        var submodels = aas.getSubmodels();
        for (var submodel : submodels.values()) {
            if (submodel.getSemanticId() != null) {
                if (submodel.getSemanticId().getKeys() != null) {
                    if (submodel.getSemanticId().getKeys().size() > 0) {
                        submodel.getSemanticId().getKeys().get(0).getValue().equals(smSemanticId);
                        var submodelElement = submodel.getSubmodelElement(smeId);
                        var submodelElementLocalCopy = submodelElement.getLocalCopy();
                        var value = submodelElementLocalCopy.getValue();

                        return value.toString();
                    }
                }
            }
        }

        return "not found";
    }

    public String findSubmodelByPropertyValue(String propertyId, String propertyValue) {
        var foundSubmodels = new ArrayList<ISubmodel>();

        var allAAS = aasManager.retrieveAASAll();
        for (var aas : allAAS) {
            var submodels = aas.getSubmodels();
            for (var submodel : submodels.values()) {
                var submodelElements = submodel.getSubmodelElements();
                for (var submodelElement : submodelElements.values()) {
                    if (submodelElement.getIdShort().equals(propertyId)) {
                        if (submodelElement.getValue().equals(propertyValue)) {
                            foundSubmodels.add(submodel);
                        }
                    }
                }
            }
        }

        if (foundSubmodels.size() > 0) {
            return foundSubmodels.get(0).getIdentification().getId();
        }
        else {
            return "Not found";
        }
    }

}
