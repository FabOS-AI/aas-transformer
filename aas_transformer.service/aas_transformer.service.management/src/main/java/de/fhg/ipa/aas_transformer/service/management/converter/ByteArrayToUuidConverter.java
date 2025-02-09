package de.fhg.ipa.aas_transformer.service.management.converter;

import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ByteArrayToUuidConverter extends AbstractConverter implements Converter<byte[], UUID> {
    @Override
    public UUID convert(byte[] source) {
        return UUID.nameUUIDFromBytes(source);
    }
}
