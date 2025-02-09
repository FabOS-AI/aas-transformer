package de.fhg.ipa.aas_transformer.aas;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AasUtils {
    private static final Logger LOG = LoggerFactory.getLogger(AasUtils.class);

    private static JsonDeserializer jsonDeserializer = new JsonDeserializer();
    private static JsonSerializer jsonSerializer = new JsonSerializer();

    public static Submodel createClone(Submodel submodel) {
        try {
            String submodelJson = jsonSerializer.write(submodel);
            Submodel clone = jsonDeserializer.read(submodelJson, Submodel.class);
            return clone;
        } catch (SerializationException e) {
            LOG.error("Error while serializing submodel | {}", e.getMessage());
            return null;
        } catch (DeserializationException e) {
            LOG.error("Error while deserializing submodel | {}", e.getMessage());
            return null;
        }
    }
}
