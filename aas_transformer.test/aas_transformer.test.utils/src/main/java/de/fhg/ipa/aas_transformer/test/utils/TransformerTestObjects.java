package de.fhg.ipa.aas_transformer.test.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.fhg.ipa.aas_transformer.model.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TransformerTestObjects {
    private static ObjectMapper objectMapper = new ObjectMapper();

    public static List<Transformer> getSimpleTransformers(int count) {
        List<Transformer> transformers = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Transformer transformer = getSimpleTransformer("_"+i);
            transformers.add(transformer);
        }
        return transformers;
    }

    public static Transformer getSimpleTransformer() {
        return getSimpleTransformer("");
    }

    private static Transformer getSimpleTransformer(String destinationSubmodelIdSuffix) {
        List<TransformerAction> actions = new ArrayList<>();
        actions.addAll(getTransformerCopyActions(5));
        DestinationSubmodel destinationSubmodel = new DestinationSubmodel(
                "new_submodel"+destinationSubmodelIdSuffix,
                "ipa.fraunhofer.de/new_submodel"+destinationSubmodelIdSuffix
        );
        Destination destination  = new Destination(destinationSubmodel);

        return new Transformer(
                destination,
                actions,
                List.of(new SourceSubmodelIdRule(
                        RuleOperator.EQUALS,
                        new SubmodelId(SubmodelIdType.ID_SHORT, "new_submodel"+destinationSubmodelIdSuffix)
                ))
        );
    }

    public static Transformer getAnsibleFactsTransformer() {
        // Create Actions:
        List<TransformerAction> actions = new ArrayList<>();
        actions.add(new TransformerActionCopy(
                "distribution",
                "distribution_new"
        ));
        actions.add(new TransformerActionCopy(
                "distribution_release",
                "distribution_release_new"
        ));

        // Create Destination Submodel
        DestinationSubmodel destinationSubmodel = new DestinationSubmodel(
                "operating_system_copy",
                "{{destinationShells:shell_id(DESTINATION_SHELLS, 0)}}/operating_system_copy"
        );
        Destination destination  = new Destination(destinationSubmodel);

        // Return Transformer:
        return new Transformer(
                destination,
                actions,
                List.of(new SourceSubmodelIdRule(
                        RuleOperator.EQUALS,
                        new SubmodelId(SubmodelIdType.ID_SHORT, "ansible_facts")
                ))
        );
    }

    public static Transformer getTimeseriesTransformer(String sourceSubmodelIdShort) {
        return new Transformer(
                UUID.randomUUID(),
                new Destination(new DestinationSubmodel(
                        "{{ submodel:idShort(SOURCE_SUBMODEL) }}_avg",
                        "{{ submodel:id(SOURCE_SUBMODEL) }}_avg")
                ),
                List.of(new TransformerActionTsAvg(
                        List.of("sensor0", "sensor1"),
                        5)
                ),
                List.of(new SourceSubmodelIdRule(RuleOperator.EQUALS,  new SubmodelId(SubmodelIdType.ID_SHORT, sourceSubmodelIdShort)))
        );
    }

    public static String getDestinationSubmodelIdOfAnsibleFactsTransformer(
            Transformer ansibleFactsTransformer,
            String destinationShellId
    ) {
        DestinationSubmodel destinationSubmodel = ansibleFactsTransformer.getDestination().getSubmodelDestination();
        String regexp = "\\{\\{.*\\}\\}";
        var regExPattern = Pattern.compile(regexp);
        return regExPattern.matcher(destinationSubmodel.getId()).replaceFirst(destinationShellId);
    }

    public static TransformerDTOListener getAnsibleFactsTransformerDTOListener() {
        Transformer factsTransformer = getAnsibleFactsTransformer();

        return  new TransformerDTOListener(
            factsTransformer.getId(),
            factsTransformer.getSourceSubmodelIdRules()
        );
    }

    public static List<Transformer> getAnsibleFactsTransformersAsList() {
        return List.of(getAnsibleFactsTransformer());
    }

    public static String getAnsibleFactsTransformerAsJsonString() {
        Transformer transformer = getAnsibleFactsTransformer();
        try {
            return objectMapper.writeValueAsString(transformer);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String getAnsibleFactsTransformerListAsJsonString() {
        List<Transformer> transformer = getAnsibleFactsTransformersAsList();
        try {
            return objectMapper.writeValueAsString(transformer);
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    public static List<TransformerAction> getTransformerCopyActions(int count) {
        List<TransformerAction> actions = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String sourceSubmodelElementId = "SimpleProperty"+i;
            TransformerActionCopy transformerAction = new TransformerActionCopy(sourceSubmodelElementId);
            actions.add(transformerAction);
        }
        return actions;
    }
}
