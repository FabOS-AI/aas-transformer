package de.fhg.ipa.aas_transformer.service.templating;

import com.hubspot.jinjava.lib.fn.InjectedContextFunctionProxy;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import org.eclipse.basyx.submodel.metamodel.api.ISubmodel;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IdentifierType;
import org.eclipse.basyx.submodel.metamodel.map.identifier.Identifier;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElementCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
public class SubmodelTemplateFunctions extends AbstractTemplateFunctions {

    private static final Logger LOG = LoggerFactory.getLogger(SubmodelTemplateFunctions.class);

    public final static String NAMESPACE = "submodel";

    public final SubmodelRegistry submodelRegistry;

    public final SubmodelRepository submodelRepository;

    public SubmodelTemplateFunctions(SubmodelRegistry submodelRegistry, SubmodelRepository submodelRepository) {
        this.submodelRegistry = submodelRegistry;
        this.submodelRepository = submodelRepository;

        try {
            var getSubmodelElementValueOfSubmodelMethodRef = SubmodelTemplateFunctions.class.getDeclaredMethod(
                    "getSubmodelElementValueOfSubmodel",
                    String.class, String.class);
            var submodelElementValueOfSubmodelTemplateMethod = InjectedContextFunctionProxy.defineProxy(
                    SubmodelTemplateFunctions.NAMESPACE, "sme_value",
                    getSubmodelElementValueOfSubmodelMethodRef, this);
            this.templateFunctions.add(submodelElementValueOfSubmodelTemplateMethod);

            var getIdOfSubmodelMethodRef = SubmodelTemplateFunctions.class.getDeclaredMethod(
                    "getSubmodelIdOfSubmodel", String.class);
            var idOfSubmodelTemplateMethod = InjectedContextFunctionProxy.defineProxy(
                    SubmodelTemplateFunctions.NAMESPACE, "id",
                    getIdOfSubmodelMethodRef, this);
            this.templateFunctions.add(idOfSubmodelTemplateMethod);

//            var resolveToSubmodelElementValueBySemanticIdMethodRef = SubmodelTemplateFunctions.class.getDeclaredMethod(
//                    "resolveToSubmodelElementValueBySemanticId",
//                    String.class, String.class, String.class);
//            var smeValueBySemanticIdTemplateMethod = InjectedContextFunctionProxy.defineProxy(
//                    SubmodelTemplateFunctions.NAMESPACE, "sme_value_semantic",
//                    resolveToSubmodelElementValueBySemanticIdMethodRef, this);
//            this.templateFunctions.add(smeValueBySemanticIdTemplateMethod);
//
//            var findSubmodelByPropertyValueMethodRef = SubmodelTemplateFunctions.class.getDeclaredMethod(
//                    "findSubmodelByPropertyValue",
//                    String.class, String.class);
//            var findSubmodelByPropTemplateMethod = InjectedContextFunctionProxy.defineProxy(
//                    SubmodelTemplateFunctions.NAMESPACE, "find_sm_by_prop",
//                    findSubmodelByPropertyValueMethodRef, this);
//            this.templateFunctions.add(findSubmodelByPropTemplateMethod);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    public String getSubmodelIdOfSubmodel(String serializedSubmodel) {
        try {
            var jsonDeserializer = new JsonDeserializer();
            var sourceSubmodel = jsonDeserializer.read(serializedSubmodel, Submodel.class);
            return sourceSubmodel.getId();
        } catch (DeserializationException e) {
            LOG.error(e.getMessage());
            return "ERROR: " + e.getMessage();
        }
    }

    public String getSubmodelElementValueOfSubmodel(String serializedSubmodel, String smeIdShortPath) {
        try {
            var jsonDeserializer = new JsonDeserializer();
            var sourceSubmodel = jsonDeserializer.read(serializedSubmodel, Submodel.class);
            var submodelElementOptional = this.findSubmodelElement(sourceSubmodel.getSubmodelElements(), smeIdShortPath);
            if (submodelElementOptional.isPresent()) {
                var submodelElement = submodelElementOptional.get();
                 if (submodelElement instanceof Property) {
                    var value = ((Property) submodelElement).getValue();
                    return value;
                }
                 else {
                     return "Submodel Element [id='" + smeIdShortPath + "'] not a property";
                 }
            }
            return "ERROR: Submodel Element [id='" + smeIdShortPath + "'] not found";
        } catch (DeserializationException e) {
           LOG.error(e.getMessage());
           return "ERROR: " + e.getMessage();
        }
    }

    private Optional<SubmodelElement> findSubmodelElement(List<SubmodelElement> submodelElements, String smeIdShortPath) {
        String idOfNextSubmodelElement;
        var pathSegments = smeIdShortPath.split("\\.");
        if (pathSegments.length > 1) {
            idOfNextSubmodelElement = pathSegments[0];
            smeIdShortPath = smeIdShortPath.replaceFirst(idOfNextSubmodelElement + ".", "");
        }
        else {
            idOfNextSubmodelElement = smeIdShortPath;
        }

        var submodelElementOptional = submodelElements.stream()
                .filter(sme -> sme.getIdShort().equals(idOfNextSubmodelElement)).findAny();
        if (submodelElementOptional.isPresent()) {
            var submodelElement = submodelElementOptional.get();
            if (submodelElement instanceof SubmodelElementCollection) {
                var nestedSubmodelElements = ((SubmodelElementCollection)submodelElement).getValue();
                return this.findSubmodelElement(nestedSubmodelElements, smeIdShortPath);
            }
            else {
                return Optional.of(submodelElement);
            }
        }

        return Optional.empty();
    }

    public String resolveToSubmodelElementValue(String aasId, String smId, String smeId) {
        var aasIdentifier = new Identifier(IdentifierType.CUSTOM, aasId);
        var smIdentifier = new Identifier(IdentifierType.CUSTOM, smId);

//        try {
//
//            var aas = submodelRepository.retrieveAAS(aasIdentifier);
//            var submodel = aas.getSubmodel(smIdentifier);
//            var submodelElement = submodel.getSubmodelElement(smeId);
//            var submodelElementLocalCopy = submodelElement.getLocalCopy();
//            var value = submodelElementLocalCopy.getValue();
//
//            return value.toString();
//        }
//        catch (Exception e) {
//            LOG.error(e.getMessage());
//            throw e;
//        }
        return "";

    }

    public String resolveToSubmodelElementValueBySemanticId(String aasId, String smSemanticId, String smeId) {
        var aasIdentifier = new Identifier(IdentifierType.CUSTOM, aasId);

//        var aas = submodelRepository.retrieveAAS(aasIdentifier);
//        var submodels = aas.getSubmodels();
//        for (var submodel : submodels.values()) {
//            if (submodel.getSemanticId() != null) {
//                if (submodel.getSemanticId().getKeys() != null) {
//                    if (submodel.getSemanticId().getKeys().size() > 0) {
//                        submodel.getSemanticId().getKeys().get(0).getValue().equals(smSemanticId);
//                        var submodelElement = submodel.getSubmodelElement(smeId);
//                        var submodelElementLocalCopy = submodelElement.getLocalCopy();
//                        var value = submodelElementLocalCopy.getValue();
//
//                        return value.toString();
//                    }
//                }
//            }
//        }

        return "not found";
    }

    public String findSubmodelByPropertyValue(String propertyId, String propertyValue) {
        var foundSubmodels = new ArrayList<ISubmodel>();

//        var allAAS = submodelRepository.retrieveAASAll();
//        for (var aas : allAAS) {
//            var submodels = aas.getSubmodels();
//            for (var submodel : submodels.values()) {
//                var submodelElements = submodel.getSubmodelElements();
//                for (var submodelElement : submodelElements.values()) {
//                    if (submodelElement.getIdShort().equals(propertyId)) {
//                        if (submodelElement.getValue().equals(propertyValue)) {
//                            foundSubmodels.add(submodel);
//                        }
//                    }
//                }
//            }
//        }

        if (foundSubmodels.size() > 0) {
            return foundSubmodels.get(0).getIdentification().getId();
        }
        else {
            return "Not found";
        }
    }

}
