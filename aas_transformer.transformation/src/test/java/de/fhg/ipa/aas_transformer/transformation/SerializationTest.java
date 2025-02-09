package de.fhg.ipa.aas_transformer.transformation;

import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.SerializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonSerializer;
import org.eclipse.digitaltwin.aas4j.v3.model.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.getSimpleShell;
import static org.junit.Assert.assertNotNull;

@Disabled
public class SerializationTest {
    @Test
    public void testSerializationOfAasObjetsInList() throws SerializationException, DeserializationException {
        JsonSerializer jsonSerializer = new JsonSerializer();
        JsonDeserializer jsonDeserializer = new JsonDeserializer();

        List<DefaultAssetAdministrationShell> shells = new ArrayList<>();
        shells.add(getSimpleShell("", ""));
        shells.add(getSimpleShell("", ""));

        String serializedShells = jsonSerializer.write(shells);

        List<AssetAdministrationShell> deserializedShells = List.of(jsonDeserializer.read(serializedShells, AssetAdministrationShell[].class));

        for(AssetAdministrationShell shell : deserializedShells) {
            assertNotNull(shell.getId());
        }

        return;
    }
}
