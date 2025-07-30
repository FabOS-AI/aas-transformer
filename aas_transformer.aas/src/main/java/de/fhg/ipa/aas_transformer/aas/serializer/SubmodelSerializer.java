package de.fhg.ipa.aas_transformer.aas.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;

import java.io.IOException;

public class SubmodelSerializer extends JsonSerializer<Submodel> {
    org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer jsonSerializer = new org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer();

    @Override
    public void serialize(Submodel submodel, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeTree(jsonSerializer.toNode(submodel));
    }
}
