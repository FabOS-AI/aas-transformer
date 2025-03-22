package de.fhg.ipa.aas_transformer.aas.serializer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;

import java.io.IOException;

public class SubmodelElementSerializer extends StdSerializer<SubmodelElement> {
    JsonSerializer jsonSerializer = new JsonSerializer();

    public SubmodelElementSerializer(Class<SubmodelElement> t) {
        super(t);
    }

    @Override
    public void serialize(SubmodelElement submodel, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        try {
            jsonGenerator.writeRaw(jsonSerializer.write(submodel));
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }
}
