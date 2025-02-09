package de.fhg.ipa.aas_transformer.transformation.templating;

import com.hubspot.jinjava.lib.fn.ELFunctionDefinition;
import com.hubspot.jinjava.lib.fn.InjectedContextFunctionProxy;
import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import org.eclipse.basyx.aas.metamodel.map.AssetAdministrationShell;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.json.JsonDeserializer;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultAssetAdministrationShell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.List;

@Component
public class DestinationShellsFunctions extends AbstractTemplateFunctions {
    private static final Logger LOG = LoggerFactory.getLogger(DestinationShellsFunctions.class);

    public final static String NAMESPACE = "destinationShells";

    public final AasRegistry aasRegistry;
    public final AasRepository aasRepository;

    private final JsonDeserializer jsonDeserializer = new JsonDeserializer();

    public DestinationShellsFunctions(AasRegistry aasRegistry, AasRepository aasRepository) {
        this.aasRegistry = aasRegistry;
        this.aasRepository = aasRepository;

        try {
            Method getIdOfDestinationShellMethodRef = DestinationShellsFunctions.class.getDeclaredMethod(
                    "getIdOfDestinationShell",
                    String.class,
                    int.class
            );
            ELFunctionDefinition getIdOfDestinationShellTemplateMethod = InjectedContextFunctionProxy.defineProxy(
                    DestinationShellsFunctions.NAMESPACE,
                    "shell_id",
                    getIdOfDestinationShellMethodRef,
                    this
            );
            this.templateFunctions.add(getIdOfDestinationShellTemplateMethod);
        } catch (NoSuchMethodException e) {
            LOG.error("Could not find method DestinationShellsFunctions.class", e);
        }
    }

    public String getIdOfDestinationShell(String serializedDestinationShells, int shellListIndex) {
        LOG.info("Getting ID of destination shell with index {}", shellListIndex);
        try {
            List<AssetAdministrationShell> destinationShells = List.of(
                    jsonDeserializer.read(
                            serializedDestinationShells,
                            AssetAdministrationShell[].class
                    )
            );
            String id = destinationShells.get(shellListIndex).get("id").toString();
            return id;
        } catch (DeserializationException e) {
            LOG.error("Could not deserialize destination shells", e);
        }
        return serializedDestinationShells;
    }
}
