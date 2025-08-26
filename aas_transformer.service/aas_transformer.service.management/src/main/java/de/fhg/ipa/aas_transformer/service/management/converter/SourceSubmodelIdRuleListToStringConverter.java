package de.fhg.ipa.aas_transformer.service.management.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.fhg.ipa.aas_transformer.model.SourceSubmodelIdRule;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SourceSubmodelIdRuleListToStringConverter extends AbstractConverter implements Converter<List<SourceSubmodelIdRule>, String> {
    @Override
    public String convert(List<SourceSubmodelIdRule> source) {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
