package de.fhg.ipa.aas_transformer.test.utils;

import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.model.DestinationSubmodel;
import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import de.fhg.ipa.aas_transformer.model.TransformerActionCopy;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.*;
import org.eclipse.digitaltwin.basyx.aasregistry.client.ApiException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getAnsibleFactsTransformer;
import static java.lang.Thread.sleep;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AasTestObjects {
    public static JsonDeserializer jsonDeserializer = new JsonDeserializer();

    public static DefaultAssetAdministrationShell getSimpleShell(String id, String idShort) {
        String shellId;
        if(id == "")
            shellId = UUID.randomUUID().toString();
        else
            shellId = id;

        String shellIdShort;
        if(idShort == "")
            if(id == "")
                shellIdShort = UUID.randomUUID().toString().split("-")[0];
            else
                shellIdShort = id.split("-")[0];
        else
            shellIdShort = idShort;

        return new DefaultAssetAdministrationShell.Builder()
            .id(shellId)
            .idShort(shellIdShort)
            .build();
    }

    public static Submodel getAnsibleFactsSubmodel() {
        try {
//            ClassLoader classLoader = AasTestObjects.class.getClassLoader();
//            URL resourceUrl = classLoader.getResource("ansible_facts.json");
//            File file = new File(resourceUrl.getFile());
            InputStream in = AasTestObjects.class.getClassLoader().getResourceAsStream("ansible_facts.json");

            var submodel = jsonDeserializer.read(
                    in,
                    Submodel.class
            );

            submodel.setId(
                submodel.getId()+"_"+getRandomString(5)
            );

            return submodel;
        } catch(DeserializationException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Submodel getRandomAnsibleFactsSubmodel() {
        Submodel submodel = getAnsibleFactsSubmodel();

        submodel.setSubmodelElements(
            submodel
                .getSubmodelElements()
                .stream()
                .map(submodelElement -> {
                    if(submodelElement instanceof DefaultProperty) {
                        DefaultProperty se = (DefaultProperty) submodelElement;
                        se.setValue(se.getValue()+"_"+getRandomString(5));
                        return se;
                    }
                    return submodelElement;
                })
                .collect(Collectors.toList())
        );

        return submodel;
    }

    public static List<List<Object>> getRandomAnsibleFactsTriples(int length) {
        List<List<Object>> triples = new ArrayList<>();

        for(int i = 0; i < length; i++) {
            List<Object> triple = new ArrayList<>();
            triple.add(getSimpleShell("", ""));
            triple.add(getRandomAnsibleFactsSubmodel());
            triple.add(getAnsibleFactsTransformer());
            try {
                triple.add(getDestinationSubmodel((Submodel) triple.get(1), (Transformer)triple.get(2)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            triples.add(triple);
        }

        return triples;
    }

    public static void registerShellAndSubmodel(
            AasRegistry aasRegistry,
            AasRepository aasRepository,
            SubmodelRegistry smRegistry,
            SubmodelRepository smRepository,
            AssetAdministrationShell shell,
            Submodel submodel
    ) {
        aasRepository.createAasOrDoNothing(shell);
        aasRepository.addSubmodelReferenceToAas(shell.getId(), submodel);
        smRepository.createOrUpdateSubmodel(submodel);
        try {
            aasRegistry.addSubmodelDescriptorToAas(
                    shell.getId(),
                    smRegistry.findSubmodelDescriptor(submodel.getId()).get()
            );
        } catch (ApiException e) {
            throw new RuntimeException(e);
        }
    }

    public static void registerAasObjectsFromTriples(
        AasRegistry aasRegistry,
        AasRepository aasRepository,
        SubmodelRegistry smRegistry,
        SubmodelRepository smRepository,
        List<List<Object>> triples
    ) {
        for(List<Object>triple : triples) {
            var shell = (AssetAdministrationShell) triple.get(0);
            var sourceSubmodel = (Submodel) triple.get(1);
            registerShellAndSubmodel(aasRegistry, aasRepository, smRegistry, smRepository, shell, sourceSubmodel);
        }
    }

    public static void deleteSourceSubmodelsFromTriples(
            AasRegistry aasRegistry,
            AasRepository aasRepository,
            SubmodelRepository smRepository,
            List<List<Object>> triples
    ) {
        for(List<Object>triple : triples) {
            AssetAdministrationShell shell = ((AssetAdministrationShell) triple.get(0));
            Submodel sourceSubmodel = (Submodel) triple.get(1);

            aasRegistry.removeSubmodelDescriptorFromAas(
                    shell.getId(),
                    sourceSubmodel.getId()
            );

            aasRepository.removeSubmodelReferenceFromAas(
                    shell.getId(),
                    sourceSubmodel.getId()
            );

            smRepository.deleteSubmodel(sourceSubmodel.getId());
        }
    }


    public static void assertExpectedSubmodelCount(
            AasRegistry aasRegistry,
            AasRepository aasRepository,
            SubmodelRepository smRepository,
            String shellId,
            int expectedTotalSubmodelCount,
            Integer expectedShellSubmodelCount
    ) throws DeserializationException, InterruptedException, ApiException {
        int tryCount = 0;
        int maxTries = 1000;
        int sleepInMs = 100;

        while(tryCount <= maxTries && smRepository.getAllSubmodels().size() != expectedTotalSubmodelCount) {
            sleep(sleepInMs);
            tryCount++;
        }

        assertEquals(
                expectedTotalSubmodelCount,
                smRepository.getAllSubmodels().size(),
                "Expected total submodel count not reached"
        );

        // Assert Submodel References/Descriptors In Shell(descriptor):
        if(expectedShellSubmodelCount != null) {
            tryCount = 0;
            while(tryCount <= maxTries && aasRepository.getAas(shellId).getSubmodels().size() != expectedShellSubmodelCount) {
                sleep(sleepInMs);
                tryCount++;
            }

            assertEquals(expectedShellSubmodelCount, aasRepository.getAas(shellId).getSubmodels().size());

            tryCount = 0;
            while(tryCount <= maxTries && aasRegistry.getAasDescriptor(shellId).get().getSubmodelDescriptors().size() != expectedShellSubmodelCount) {
                sleep(sleepInMs);
                tryCount++;
            }

            assertEquals(expectedShellSubmodelCount, aasRegistry.getAasDescriptor(shellId).get().getSubmodelDescriptors().size());
        }
    }

    public static Submodel getOperatingSystemSubmodel() {
        DefaultSubmodel submodel = new DefaultSubmodel();
        Transformer ansibleFactsTransformer = getAnsibleFactsTransformer();
        DestinationSubmodel destinationSubmodel = ansibleFactsTransformer.getDestination().getSubmodelDestination();

        submodel.setId(destinationSubmodel.getId());
        submodel.setIdShort(destinationSubmodel.getIdShort());

        return submodel;
    }

    public static Submodel getDestinationSubmodel(Submodel sourceSubmodel, Transformer transformer) throws Exception {
        DefaultSubmodel submodel = new DefaultSubmodel();
        submodel.setId(
                transformer.getDestination().getSubmodelDestination().getId()
        );
        submodel.setIdShort(
                transformer.getDestination().getSubmodelDestination().getIdShort()
        );

        for(TransformerAction action : transformer.getTransformerActions()){
            if(action instanceof TransformerActionCopy) {
                TransformerActionCopy actionCopy = (TransformerActionCopy) action;
                Optional<SubmodelElement> optionalSourceSubmodelElement = sourceSubmodel
                        .getSubmodelElements()
                        .stream()
                        .filter(se -> se.getIdShort().equals(actionCopy.getSourceSubmodelElement()))
                        .findFirst();
                if(optionalSourceSubmodelElement.isPresent()) {
                    DefaultProperty sourceSubmodelElement = (DefaultProperty) optionalSourceSubmodelElement.get();
                    DefaultProperty targetSubmodelElement = new DefaultProperty();
                    targetSubmodelElement.setIdShort(actionCopy.getDestinationSubmodelElement());
                    targetSubmodelElement.setValue(sourceSubmodelElement.getValue());
                    submodel.getSubmodelElements().add(targetSubmodelElement);
                }
            } else {
                throw new Exception("Action type " + action.getActionType() + " not supported");
            }
        }

        return submodel;
    }

    private static Reference getSemanticIdReference(String semanticIdUrl) {
        DefaultReference semanticId = new DefaultReference();
        DefaultKey semanticIdKey = new DefaultKey();
        semanticIdKey.setType(KeyTypes.GLOBAL_REFERENCE);
        semanticIdKey.setValue(semanticIdUrl);
        semanticId.setType(ReferenceTypes.EXTERNAL_REFERENCE);
        semanticId.setKeys(List.of(semanticIdKey));
        return semanticId;
    }

    private static String getRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < length; i++) {
            result.append(characters.charAt((int) Math.floor(Math.random() * characters.length())));
        }
        return result.toString();
    }
}
