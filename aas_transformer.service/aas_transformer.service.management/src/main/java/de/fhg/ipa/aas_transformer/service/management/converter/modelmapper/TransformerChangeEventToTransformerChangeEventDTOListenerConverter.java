package de.fhg.ipa.aas_transformer.service.management.converter.modelmapper;

import de.fhg.ipa.aas_transformer.model.Transformer;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEvent;
import de.fhg.ipa.aas_transformer.model.TransformerChangeEventDTOListener;
import de.fhg.ipa.aas_transformer.model.TransformerDTOListener;
import org.modelmapper.AbstractConverter;

public class TransformerChangeEventToTransformerChangeEventDTOListenerConverter extends
        AbstractConverter<TransformerChangeEvent, TransformerChangeEventDTOListener> {

    private static TransformerToTransformerDTOListenerConverter converter = new TransformerToTransformerDTOListenerConverter();

    @Override
    protected TransformerChangeEventDTOListener convert(TransformerChangeEvent transformerChangeEvent) {
        return new TransformerChangeEventDTOListener(
                transformerChangeEvent.getType(),
                converter.convert(transformerChangeEvent.getTransformer())
        );
    }
}
