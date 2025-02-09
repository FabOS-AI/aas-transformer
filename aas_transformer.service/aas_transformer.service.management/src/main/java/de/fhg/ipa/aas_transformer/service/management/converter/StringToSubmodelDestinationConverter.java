package de.fhg.ipa.aas_transformer.service.management.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.fhg.ipa.aas_transformer.model.DestinationSubmodel;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToSubmodelDestinationConverter extends AbstractConverter implements Converter<String, DestinationSubmodel> {
    @Override
    public DestinationSubmodel convert(String source) {
        try {
            return objectMapper.readValue(source, DestinationSubmodel.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
