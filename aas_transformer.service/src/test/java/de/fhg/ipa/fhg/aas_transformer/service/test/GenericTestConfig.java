package de.fhg.ipa.fhg.aas_transformer.service.test;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.model.*;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import org.eclipse.basyx.aas.manager.ConnectedAssetAdministrationShellManager;
import org.eclipse.basyx.aas.metamodel.map.AssetAdministrationShell;
import org.eclipse.basyx.aas.metamodel.map.descriptor.CustomId;
import org.eclipse.basyx.aas.metamodel.map.parts.Asset;
import org.eclipse.basyx.submodel.metamodel.api.identifier.IIdentifier;
import org.eclipse.basyx.submodel.metamodel.map.Submodel;
import org.eclipse.basyx.submodel.metamodel.map.qualifier.Referable;
import org.eclipse.basyx.submodel.metamodel.map.submodelelement.SubmodelElement;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class GenericTestConfig {
    public static int AAS_REGISTRY_PUBLIC_PORT = 4000;
    public static int AAS_SERVER_PUBLIC_PORT = 4001;
    public static ConnectedAssetAdministrationShellManager aasManager;
    public static AASRegistry aasRegistry;
    public static ObjectMapper objectMapper = new ObjectMapper();
    public static AssetAdministrationShell AAS = new AssetAdministrationShell(
            "aas_id_short",
            new CustomId("aas-id"),
            new Asset()
    );

    public static File ansibleFactsSubmodelFile = new File("src/test/resources/ansible-facts-submodel.json");
    public static File ansibleFactsSubmodelElementFile = new File("src/test/resources/ansible-facts-submodel-elements.json");
    public static File ansibleFactsSubmodelAndSubmodelElementFile = new File("src/test/resources/ansible-facts-submodel-incl-submodel-elements.json");


    public static String getAASRegistryUrl() {
        return "http://localhost:"+AAS_REGISTRY_PUBLIC_PORT+"/registry";
    }
    public static String getAASServerUrl() {
        return "http://localhost:"+AAS_SERVER_PUBLIC_PORT+"/aasServer";
    }

    public static Submodel getAnsibleFactsSubmodelWithoutSubmodelElements() throws IOException {
        Submodel ansibleFactsSubmodel = objectMapper.readValue(
                ansibleFactsSubmodelFile,
                Submodel.class
        );

        return  ansibleFactsSubmodel;
    }

    public static Submodel getAnsibleFactsSubmodel() throws IOException {
        Submodel ansibleFactsSubmodel = getAnsibleFactsSubmodelWithoutSubmodelElements();

        addSubmodelElements(ansibleFactsSubmodel);

        return ansibleFactsSubmodel;
    }

    public static IIdentifier getAnsibleFactsIdentifier() throws IOException {
        return getAnsibleFactsSubmodel().getIdentification();
    }

    public static List<SubmodelElement> getAnsibleFactsSubmodelElements() throws IOException {
        List<SubmodelElement> submodelElements = objectMapper.readValue(
                ansibleFactsSubmodelElementFile,
                new TypeReference<>(){}
        );

        return submodelElements;
    }

    public static CustomId getDestinationSubmodelIdentifier() {
        return new CustomId(
                osIdShort+"-"+AAS.getIdentification().getId()
        );
    }

    private static void addSubmodelElements(Submodel ansibleFactsSubmodel) throws IOException {
        List<SubmodelElement> submodelElements = getAnsibleFactsSubmodelElements();

        for(SubmodelElement se : submodelElements) {
            ((Map)ansibleFactsSubmodel.get(Submodel.SUBMODELELEMENT)).put(
                    se.get(Referable.IDSHORT),
                    se
            );
        }
    }

    private static String osIdShort = "operating_system";
    private static Destination destination = new Destination();
    private static SubmodelIdentifier osSourceSubmodelIdentifier = new SubmodelIdentifier(
            SubmodelIdentifierType.ID_SHORT,
            "ansible-facts"
    );
    private static LinkedList<TransformerAction> osTransformerActions = new LinkedList<>() {{
        add(new TransformerActionCopy(osSourceSubmodelIdentifier,"distribution"));
        add(new TransformerActionCopy(osSourceSubmodelIdentifier,"distribution_major_version"));
    }};
    public static Transformer osTransformer;

    static {
        destination.setSmDestination(new DestinationSM(osIdShort, new CustomId(osIdShort)));

        osTransformer = new Transformer(
                destination,
                osTransformerActions
        );
    }
}
