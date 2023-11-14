package de.fhg.ipa.aas_transformer.initializer.test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import de.fhg.ipa.aas_transformer.model.Transformer;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class InitializerITConfig {
    private final static String dockerComposeFilePath = "src/test/resources/docker-compose.yml";
    private final static String jsonFilesFolderPath = "src/main/resources/json";
    public final static DockerComposeContainer dockerCompose;
    public final static String AAS_TRANSFORMER_SERVICE_NAME = "aas-transformer";
    public final static int AAS_TRANSFORMER_SERVICE_PORT = 4010;
    public static List<File> jsonFiles = new ArrayList<>();
    public static List<Transformer> transformerListFromFiles = new ArrayList<>();
    public static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.enableDefaultTyping(
                ObjectMapper.DefaultTyping.OBJECT_AND_NON_CONCRETE,
                JsonTypeInfo.As.EXISTING_PROPERTY
        );

        dockerCompose = new DockerComposeContainer(new File(dockerComposeFilePath))
                .withBuild(true)
                .withExposedService(
                        AAS_TRANSFORMER_SERVICE_NAME,
                        AAS_TRANSFORMER_SERVICE_PORT
                )
                .withLocalCompose(true);

        dockerCompose.start();

        try (Stream<Path> paths = Files.walk(Paths.get(jsonFilesFolderPath))) {
            paths
                    .filter(Files::isRegularFile)
                    .filter(f -> f.toString().endsWith(".json"))
                    .forEach(f -> {
                        jsonFiles.add(new File(f.toString()));
                    });
        } catch (IOException e) { throw new RuntimeException(e);}

        for(File file : jsonFiles) {
            try {
                transformerListFromFiles.add(
                        objectMapper.readValue(file, Transformer.class)
                );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }


}
