package de.fhg.ipa.aas_transformer.service.management.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class TransformerActionToStringConverter extends AbstractConverter implements Converter<TransformerAction, String> {
    @Override
    public String convert(TransformerAction source) {
        try {
            return objectMapper.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
