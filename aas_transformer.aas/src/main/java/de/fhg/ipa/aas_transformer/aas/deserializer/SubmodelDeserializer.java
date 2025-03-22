package de.fhg.ipa.aas_transformer.aas.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;

import java.io.IOException;

public class SubmodelDeserializer extends StdDeserializer<Submodel> {
    static ObjectMapper mapper = new ObjectMapper();
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
            JsonNode node = mapper.readTree(p);
            String nodeText = mapper.writeValueAsString(node);
            return jsonDeserializer.read(nodeText, Submodel.class);
        } catch (DeserializationException e) {
            throw new RuntimeException(e);
        }

    }

}
