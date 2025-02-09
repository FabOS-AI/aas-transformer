package de.fhg.ipa.aas_transformer.service.management.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.fhg.ipa.aas_transformer.model.TransformerAction;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToTransformerActionConverter extends AbstractConverter implements Converter<String, TransformerAction> {
    @Override
    public TransformerAction convert(String source) {
        String transformationAction = source;
        try {
            return objectMapper.readValue(source, TransformerAction.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
