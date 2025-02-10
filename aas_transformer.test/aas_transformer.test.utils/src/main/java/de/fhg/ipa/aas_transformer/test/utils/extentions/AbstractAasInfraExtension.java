package de.fhg.ipa.aas_transformer.test.utils.extentions;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

abstract class AbstractAasInfraExtension {
    protected static final String basyxImageVersion = "2.0.0-milestone-04";
    private static final Duration CONTAINER_STARTUP_TIMEOUT = Duration.ofMinutes(5);
    public static String HOSTNAME = "localhost";

    private static Network containerNetwork = Network.newNetwork();
    private static Map<String, String> containerLabels = Map.of(
            "com.docker.compose.project", "aas-transfomer-it",
            "de.fhg.ipa.aas-transformer.container-group","basyx"
    );
    private static String aasEnvMongoDbHostname = "aas-env-mongo";
    private static String aasEnvMongoDbAdminUsername = "mongoAdmin";
    private static String aasEnvMongoDbAdminPassword = "mongoPassword";

    public static String aasEnvUrlPrefix = "/aas-env";
    public static int traefikWebPort;

    public static GenericContainer getAasRegistryContainer() {
        GenericContainer container = new GenericContainer<>(
                DockerImageName.parse("eclipsebasyx/aas-registry-log-mem:"+basyxImageVersion))
                .withNetwork(containerNetwork)
                .withNetworkAliases("aas-registry")
                .withLabels(containerLabels)
                .dependsOn()
                .withExposedPorts(8080)
                .withEnv("BASYX_CORS_ALLOWEDORIGINS", "*")
                .withEnv("BASYX_CORS_ALLOWEDMETHODS", "GET,POST,PATCH,DELETE,PUT,OPTIONS,HEAD")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));

        container.setPortBindings(List.of("8080:8080"));

        return container;
    }

    public static GenericContainer getSmRegistryContainer() {
        GenericContainer container = new GenericContainer<>(
                DockerImageName.parse("eclipsebasyx/submodel-registry-log-mem:"+basyxImageVersion))
                .withNetwork(containerNetwork)
                .withNetworkAliases("sm-registry")
                .withLabels(containerLabels)
                .dependsOn()
                .withExposedPorts(8080)
                .withEnv("BASYX_CORS_ALLOWEDORIGINS", "*")
                .withEnv("BASYX_CORS_ALLOWEDMETHODS", "GET,POST,PATCH,DELETE,PUT,OPTIONS,HEAD")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));

        container.setPortBindings(List.of("8082:8080"));

        return container;
    }

    public static GenericContainer getAasEnvContainer(String externalUrl) {
        String externalPort = "8081";
        List<String> portMapping = null;
        int randomFreeHostPort;
        try {
            ServerSocket socket = new ServerSocket(0);
            randomFreeHostPort = socket.getLocalPort();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(externalUrl.equals("")) {
            externalUrl = "http://" + HOSTNAME + ":" + externalPort;
            portMapping = List.of( "8081:8081");
        }
        String mqttClientId = "aas-transformer-it_aas-env"+ UUID.randomUUID().toString().split("-")[0];

        Map<String,String> traefikLabels = Map.of(
                "traefik.enable", "true",
                "traefik.http.routers.aas-env.rule", "PathPrefix(`"+aasEnvUrlPrefix+"`)",
                "traefik.http.routers.aas-env.service", "aas-env",
                "traefik.http.routers.aas-env.middlewares", "aas-env",
                "traefik.http.services.aas-env.loadbalancer.server.port", "8081",
                "traefik.http.middlewares.aas-env.stripprefix.prefixes", aasEnvUrlPrefix,
                "traefik.docker.network", containerNetwork.getId()
        );

        Map<String,String> aasEnvLabels = new HashMap<>();
        aasEnvLabels.putAll(containerLabels);
        aasEnvLabels.putAll(traefikLabels);

        GenericContainer container = new GenericContainer<>(
                DockerImageName.parse("eclipsebasyx/aas-environment:"+basyxImageVersion))
                .withNetwork(containerNetwork)
                .withLabels(aasEnvLabels)
                .dependsOn()
                .withExposedPorts(8081)
                .withEnv("BASYX_BACKEND", "MongoDB")
                .withEnv("SPRING_DATA_MONGODB_HOST", aasEnvMongoDbHostname)
                .withEnv("SPRING_DATA_MONGODB_DATABASE", "aas-env")
                .withEnv("SPRING_DATA_MONGODB_AUTHENTICATIONDATABASE", "admin")
                .withEnv("SPRING_DATA_MONGODB_USERNAME", aasEnvMongoDbAdminUsername)
                .withEnv("SPRING_DATA_MONGODB_PASSWORD", aasEnvMongoDbAdminPassword)
                .withEnv("BASYX_AASREPOSITORY_FEATURE_MQTT_ENABLED", "true")
                .withEnv("BASYX_SUBMODELREPOSITORY_FEATURE_MQTT_ENABLED", "true")
                .withEnv("BASYX_CORS_ALLOWEDORIGINS", "*")
                .withEnv("BASYX_CORS_ALLOWEDMETHODS", "GET,POST,PATCH,DELETE,PUT,OPTIONS,HEAD")
                .withEnv("BASYX_AASREPOSITORY_FEATURE_REGISTRYINTEGRATION", "http://aas-registry:8080")
                .withEnv("BASYX_SUBMODELREPOSITORY_FEATURE_REGISTRYINTEGRATION", "http://sm-registry:8080")
                .withEnv("BASYX_EXTERNALURL", externalUrl)
                .withEnv("MQTT_CLIENTID", mqttClientId)
                .withEnv("MQTT_HOSTNAME", "mqtt")
                .withEnv("MQTT_PORT", "1883")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));

        if(portMapping != null) {
            container.setPortBindings(portMapping);
        }

        return container;
    }

    public static GenericContainer getAasEnvMongoDbContainer() {
        return new GenericContainer(
                DockerImageName.parse("mongo:5.0.10"))
                .withNetwork(containerNetwork)
                .withNetworkAliases(aasEnvMongoDbHostname)
                .withLabels(containerLabels)
                .withExposedPorts(27017)
                .withEnv("MONGO_INITDB_ROOT_USERNAME", aasEnvMongoDbAdminUsername)
                .withEnv("MONGO_INITDB_ROOT_PASSWORD", aasEnvMongoDbAdminPassword)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
    }

    public static GenericContainer getAasGuiContainer(Integer aasRegistryPort, Integer submodelRegistryPort, Integer aasEnvPort) {
        return new GenericContainer<>(
                DockerImageName.parse("eclipsebasyx/aas-gui:v2-240327"))
                .withNetwork(containerNetwork)
                .withLabel("com.docker.compose.project", "aas-transfomer-it")
                .dependsOn()
                .withExposedPorts(3000)
                .withEnv("AAS_REGISTRY_PATH", "http://localhost:" + aasRegistryPort)
                .withEnv("SUBMODEL_REGISTRY_PATH", "http://localhost:" + submodelRegistryPort)
                .withEnv("AAS_REPO_PATH", "http://localhost:" + aasEnvPort + "/shells")
                .withEnv("SUBMODEL_REPO_PATH", "http://localhost:" + aasEnvPort + "/submodels")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
    }

    public static GenericContainer getMqttBrokerContainer() {
        return new GenericContainer<>(
                DockerImageName.parse("eclipse-mosquitto:2.0.20"))
                .withNetwork(containerNetwork)
                .withNetworkAliases("mqtt")
                .withLabels(containerLabels)
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("mosquitto.conf"),
                        "/mosquitto/config/mosquitto.conf"
                )
                .dependsOn()
                .withExposedPorts(1883, 9001)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
    }

    public static GenericContainer getMqttExporterContainer() {
        return new GenericContainer(
                DockerImageName.parse("jryberg/mosquitto-exporter:v0.7.4"))
                .withNetwork(containerNetwork)
                .withNetworkAliases("mqtt-exporter")
                .withLabels(containerLabels)
                .withCommand("--endpoint tcp://mqtt:1883")
                .withExposedPorts(9234)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
    }

    public static GenericContainer getMqttGuiContainer() {
        return new GenericContainer(
                DockerImageName.parse("emqx/mqttx-web"))
                .withNetwork(containerNetwork)
                .withNetworkAliases("mqtt-gui")
                .withLabels(containerLabels)
                .withExposedPorts(80)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
    }

    public static GenericContainer getTraefikContainer() throws IOException {
        ServerSocket socket = new ServerSocket(0);
        traefikWebPort = socket.getLocalPort();
        socket.close();
        return new FixedHostPortGenericContainer("traefik:v3.0")
                .withFixedExposedPort(traefikWebPort, 80)
                .withNetwork(containerNetwork)
                .withNetworkAliases("traefik")
                .withLabels(containerLabels)
                .withFileSystemBind("/var/run/docker.sock", "/var/run/docker.sock", BindMode.READ_ONLY)
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("traefik_config.yml"),
                        "/etc/traefik/traefik.yml"
                )
                .withExposedPorts(80, 443, 8080)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
    }
}
