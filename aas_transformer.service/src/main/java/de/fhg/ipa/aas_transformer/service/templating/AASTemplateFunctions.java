package de.fhg.ipa.aas_transformer.service.templating;

import com.hubspot.jinjava.lib.fn.InjectedContextFunctionProxy;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IdentifierType;
import org.eclipse.basyx.submodel.metamodel.map.identifier.Identifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AASTemplateFunctions extends AbstractTemplateFunctions {

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
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public String resolveToSubmodelElementValue(String aasId, String smId, String smeId) {
        var aasIdentifier = new Identifier(IdentifierType.CUSTOM, aasId);
        var smIdentifier = new Identifier(IdentifierType.CUSTOM, smId);

        var aas = aasManager.retrieveAAS(aasIdentifier);
        var submodel = aas.getSubmodel(smIdentifier);
        var submodelElement = submodel.getSubmodelElement(smeId);
        var submodelElementLocalCopy = submodelElement.getLocalCopy();
        var value = submodelElementLocalCopy.getValue();

        return value.toString();
    }

}
