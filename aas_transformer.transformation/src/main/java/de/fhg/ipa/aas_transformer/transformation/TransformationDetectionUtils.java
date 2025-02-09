package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.model.RuleOperator;
import de.fhg.ipa.aas_transformer.model.SourceSubmodelIdRule;
import de.fhg.ipa.aas_transformer.model.Transformer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TransformationDetectionUtils {
    private static final Logger LOG = LoggerFactory.getLogger(TransformationDetectionUtils.class);



    public static List<Submodel> lookupSourceSubmodels(Transformer transformer, SubmodelRepository submodelRepository) {
        List<Submodel> sourceSubmodels;
        try {
            sourceSubmodels = submodelRepository.getAllSubmodels()
                    .stream()
                    .filter(s ->
                            isSubmodelSourceOfTransformer(s, transformer.getSourceSubmodelIdRules())
                    )
                    .toList();
        } catch (DeserializationException e) {
            LOG.error("Failed to lookup source submodels for transformer: {} | {}", transformer.getId(), e.getMessage());
            return new ArrayList<>();
        }
        return sourceSubmodels;
    }

    public static List<String> lookupSourceSubmodelIds(Transformer transformer, SubmodelRepository submodelRepository) {
        return  lookupSourceSubmodels(transformer, submodelRepository)
                .stream()
                .map(Submodel::getId)
                .toList();
    }

    public static boolean isSubmodelSourceOfTransformer(Submodel submodel, List<SourceSubmodelIdRule> rules) {
        boolean isSubmodelSource = true;

        for(SourceSubmodelIdRule rule : rules) {
            if(!isSource(submodel, rule)) {
                isSubmodelSource = false;
                break;
            }
        }

        return isSubmodelSource;

    }

    private static boolean isSource(Submodel submodel, SourceSubmodelIdRule rule) {
        String sourceSubmodelId;

        switch (rule.getSourceSubmodelId().getType()) {
            case ID -> {
                sourceSubmodelId = submodel.getId();
            }
            case ID_SHORT -> {
                sourceSubmodelId = submodel.getIdShort();
            }
            case SEMANTIC_ID -> {
                sourceSubmodelId = submodel.getSemanticId().getKeys().get(0).getValue();
            }
            default -> {
                LOG.warn("Unknown submodel id type: " + rule.getSourceSubmodelId().getType());
                return false;
            }

        }

        return checkSourceSubmodelIdRule(sourceSubmodelId, rule.getRuleOperator(), rule.getSourceSubmodelId().getId());
    }

    private static boolean checkSourceSubmodelIdRule(String sourceSubmodelId, RuleOperator ruleOperator, String expectedSubmodelId) {
        Pattern pattern = Pattern.compile(expectedSubmodelId);
        Matcher matcher = pattern.matcher(sourceSubmodelId);
        return ruleOperator.equals(RuleOperator.EQUALS) ? matcher.matches() : !matcher.matches();
    }


}
