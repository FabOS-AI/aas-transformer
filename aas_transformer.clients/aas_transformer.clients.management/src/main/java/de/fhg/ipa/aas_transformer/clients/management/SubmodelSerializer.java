package de.fhg.ipa.aas_transformer.clients.management;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;

import java.io.IOException;

public class SubmodelSerializer extends StdSerializer<Submodel> {
    JsonSerializer jsonSerializer = new JsonSerializer();

    protected SubmodelSerializer(Class<Submodel> t) {
        super(t);
    }

    @Override
    public void serialize(Submodel submodel, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
        try {
            jsonGenerator.writeRaw(jsonSerializer.write(submodel));
        } catch (SerializationException e) {
            throw new RuntimeException(e);
        }
    }
}
