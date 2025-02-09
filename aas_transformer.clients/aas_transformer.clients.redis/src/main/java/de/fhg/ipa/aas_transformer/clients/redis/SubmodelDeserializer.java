package de.fhg.ipa.aas_transformer.clients.redis;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;

import java.io.IOException;

public class SubmodelDeserializer extends StdDeserializer<Submodel> {
    JsonDeserializer jsonDeserializer = new JsonDeserializer();

    public SubmodelDeserializer() {
        this(null);
    }

    public SubmodelDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Submodel deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        try {
            String text = p.getText();
            return jsonDeserializer.read(text, Submodel.class);
        } catch (DeserializationException e) {
            throw new RuntimeException(e);
        }

    }

}
