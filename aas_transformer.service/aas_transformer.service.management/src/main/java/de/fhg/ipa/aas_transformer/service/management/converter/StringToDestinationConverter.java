package de.fhg.ipa.aas_transformer.service.management.converter;

import com.fasterxml.jackson.core.JsonProcessingException;
import de.fhg.ipa.aas_transformer.model.Destination;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToDestinationConverter extends AbstractConverter implements Converter<String, Destination> {
    @Override
    public Destination convert(String source) {
        try {
            return objectMapper.readValue(source, Destination.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
