package de.fhg.ipa.aas_transformer.service.management.converter;

import com.fasterxml.jackson.databind.ObjectMapper;

abstract public class AbstractConverter {
    ObjectMapper objectMapper = new ObjectMapper();
}
