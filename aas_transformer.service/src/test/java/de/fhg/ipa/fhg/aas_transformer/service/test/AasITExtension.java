package de.fhg.ipa.fhg.aas_transformer.service.test;

import com.github.dockerjava.api.model.ExposedPort;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.utility.DockerImageName;

import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class AasITExtension extends SpringExtension implements BeforeAllCallback, AfterAllCallback {

    protected Network containerNetwork = Network.newNetwork();
    protected GenericContainer mqttBrokerContainer;
    protected GenericContainer aasGuiContainer;
    protected GenericContainer aasRegistryContainer;
    protected GenericContainer smRegistryContainer;
    protected GenericContainer aasEnvContainer;
    protected List<GenericContainer> runningContainers = new ArrayList<>();

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (runningContainers.size() == 0) {

            mqttBrokerContainer = new GenericContainer<>(
                    DockerImageName.parse("ghcr.io/fabos-ai/aas-transformer/aas-broker:2.0.15"))
                    .withNetwork(containerNetwork)
                    .withNetworkAliases("mqtt")
                    .withLabel("com.docker.compose.project", "aas-transfomer-it")
                    .dependsOn()
                    .withExposedPorts(1884);
            mqttBrokerContainer.start();
            runningContainers.add(mqttBrokerContainer);

            aasRegistryContainer = new GenericContainer<>(
                    DockerImageName.parse("eclipsebasyx/aas-registry-log-mem:2.0.0-SNAPSHOT"))
                    .withNetwork(containerNetwork)
                    .withNetworkAliases("aas-registry")
                    .withLabel("com.docker.compose.project", "aas-transfomer-it")
                    .dependsOn()
                    .withExposedPorts(8080)
                    .withEnv("BASYX_CORS_ALLOWEDORIGINS", "*")
                    .withEnv("BASYX_CORS_ALLOWEDMETHODS", "GET,POST,PATCH,DELETE,PUT,OPTIONS,HEAD");
            aasRegistryContainer.start();
            runningContainers.add(aasRegistryContainer);

            smRegistryContainer = new GenericContainer<>(
                    DockerImageName.parse("eclipsebasyx/submodel-registry-log-mem:2.0.0-SNAPSHOT"))
                    .withNetwork(containerNetwork)
                    .withNetworkAliases("sm-registry")
                    .withLabel("com.docker.compose.project", "aas-transfomer-it")
                    .dependsOn()
                    .withExposedPorts(8080)
                    .withEnv("BASYX_CORS_ALLOWEDORIGINS", "*")
                    .withEnv("BASYX_CORS_ALLOWEDMETHODS", "GET,POST,PATCH,DELETE,PUT,OPTIONS,HEAD");
            smRegistryContainer.start();
            runningContainers.add(smRegistryContainer);

            var socket = new ServerSocket(0);
            var randomFreeHostPort = socket.getLocalPort();
            socket.close();
            aasEnvContainer = new FixedHostPortGenericContainer<>("eclipsebasyx/aas-environment:2.0.0-SNAPSHOT")
                    .withFixedExposedPort(randomFreeHostPort, 8081)
                    .withNetwork(containerNetwork)
                    .withLabel("com.docker.compose.project", "aas-transfomer-it")
                    .dependsOn()
                    .withExposedPorts(8081)
                    .withEnv("BASYX_AASREPOSITORY_FEATURE_MQTT_ENABLED", "true")
                    .withEnv("BASYX_SUBMODELREPOSITORY_FEATURE_MQTT_ENABLED", "true")
                    .withEnv("BASYX_CORS_ALLOWEDORIGINS", "*")
                    .withEnv("BASYX_CORS_ALLOWEDMETHODS", "GET,POST,PATCH,DELETE,PUT,OPTIONS,HEAD")
                    .withEnv("BASYX_AASREPOSITORY_FEATURE_REGISTRYINTEGRATION", "http://aas-registry:8080")
                    .withEnv("BASYX_SUBMODELREPOSITORY_FEATURE_REGISTRYINTEGRATION", "http://sm-registry:8080")
                    .withEnv("BASYX_EXTERNALURL", "http://localhost:" + randomFreeHostPort)
                    .withEnv("MQTT_CLIENTID", "aas-transformer-it_aas-env")
                    .withEnv("MQTT_HOSTNAME", "mqtt")
                    .withEnv("MQTT_PORT", "1884");
            aasEnvContainer.start();
            runningContainers.add(aasEnvContainer);

            var aasRegistryPort = aasRegistryContainer.getMappedPort(8080);
            var submodelRegistryPort = smRegistryContainer.getMappedPort(8080);
            var aasEnvPort = aasEnvContainer.getMappedPort(8081);
            var mqttBrokerPort = mqttBrokerContainer.getMappedPort(1884);

            aasGuiContainer = new GenericContainer<>(
                    DockerImageName.parse("eclipsebasyx/aas-gui:v2-240327"))
                    .withNetwork(containerNetwork)
                    .withLabel("com.docker.compose.project", "aas-transfomer-it")
                    .dependsOn()
                    .withExposedPorts(3000)
                    .withEnv("AAS_REGISTRY_PATH", "http://localhost:" + aasRegistryPort)
                    .withEnv("SUBMODEL_REGISTRY_PATH", "http://localhost:" + submodelRegistryPort)
                    .withEnv("AAS_REPO_PATH", "http://localhost:" + aasEnvPort + "/shells")
                    .withEnv("SUBMODEL_REPO_PATH", "http://localhost:" + aasEnvPort + "/submodels");
            aasGuiContainer.start();
            runningContainers.add(aasGuiContainer);

            System.setProperty("aas.broker.host", "localhost");
            System.setProperty("aas.broker.port", mqttBrokerPort.toString());
            System.setProperty("aas.aas-registry.port", aasRegistryPort.toString());
            System.setProperty("aas.aas-repository.port", aasEnvPort.toString());
            System.setProperty("aas.submodel-registry.port", submodelRegistryPort.toString());
            System.setProperty("aas.submodel-repository.port", aasEnvPort.toString());
            System.setProperty("aas.aas-registry.url", "http://localhost:" + aasRegistryPort);
            System.setProperty("aas.aas-repository.url", "http://localhost:" + aasEnvPort);
            System.setProperty("aas.submodel-registry.url", "http://localhost:" + submodelRegistryPort);
            System.setProperty("aas.submodel-repository.url", "http://localhost:" + aasEnvPort);

            super.beforeAll(context);
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (runningContainers.size() > 0 && context.getParent().get().getParent().isEmpty()) {
            for (var container : runningContainers) {
                container.stop();
            }
            runningContainers = new ArrayList<>();
        }

        super.afterAll(context);
    }
}
