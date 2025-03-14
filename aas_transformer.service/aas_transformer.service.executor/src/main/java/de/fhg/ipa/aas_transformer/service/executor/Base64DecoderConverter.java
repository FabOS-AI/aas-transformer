package de.fhg.ipa.aas_transformer.service.executor;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
public class Base64DecoderConverter implements Converter<String, String> {
    @Override
    public String convert(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        return new String(Base64.getDecoder().decode(source));
    }
}
