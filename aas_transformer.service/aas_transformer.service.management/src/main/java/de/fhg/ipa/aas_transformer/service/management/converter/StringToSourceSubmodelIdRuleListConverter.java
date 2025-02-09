package de.fhg.ipa.aas_transformer.service.management.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import de.fhg.ipa.aas_transformer.model.SourceSubmodelIdRule;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class StringToSourceSubmodelIdRuleListConverter extends AbstractConverter implements Converter<String, List<SourceSubmodelIdRule>> {
    @Override
    public List<SourceSubmodelIdRule> convert(String source) {
        try {
            return objectMapper.readValue(source, new TypeReference<List<SourceSubmodelIdRule>>(){});
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
