package de.fhg.ipa.aas_transformer.service.management.converter.modelmapper;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import org.modelmapper.AbstractConverter;

import java.util.stream.Collectors;

public class TransformerToTransformerDTOListenerConverter extends AbstractConverter<Transformer, TransformerDTOListener> {
    @Override
    protected TransformerDTOListener convert(Transformer transformer) {
        return new TransformerDTOListener(
                transformer.getId(),
                transformer.getSourceSubmodelIdRules()
        );
    }
}
