package de.fhg.ipa.aas_transformer.aas.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
//import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;

import java.io.IOException;

public class SubmodelSerializer extends JsonSerializer<Submodel> {
    org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer jsonSerializer = new org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer();

    @Override
    public void serialize(Submodel submodel, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeTree(jsonSerializer.toNode(submodel));
    }
}
