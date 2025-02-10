package de.fhg.ipa.aas_transformer.test.utils.extentions;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.test.context.junit.jupiter.SpringExtension;
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
import java.util.*;

public class AasITExtension extends AbstractExtension implements BeforeAllCallback, AfterAllCallback {

    private static final Duration CONTAINER_STARTUP_TIMEOUT = Duration.ofMinutes(5);
    private static boolean RUN_LOAD_BALANCER = true;
    private static String HOSTNAME = "localhost";

    protected Network containerNetwork = Network.newNetwork();
    protected GenericContainer traefikContainer;
    protected GenericContainer mqttBrokerContainer;
    protected GenericContainer mqttExporterContainer;
    protected GenericContainer mqttGuiContainer;
    protected GenericContainer aasGuiContainer;
    protected GenericContainer aasRegistryContainer;
    protected GenericContainer smRegistryContainer;
    protected List<GenericContainer> aasEnvContainers;
    protected GenericContainer aasEnvMongoDbContainer;
    protected List<GenericContainer> runningContainers = new ArrayList<>();

    Map<String, String> containerLabels = Map.of(
            "com.docker.compose.project", "aas-transfomer-it",
            "de.fhg.ipa.aas-transformer.container-group","basyx"
    );

    String aasEnvUrlPrefix = "/aas-env";
    String aasEnvMongoDbHostname = "aas-env-mongo";
    String aasEnvMongoDbAdminUsername = "mongoAdmin";
    String aasEnvMongoDbAdminPassword = "mongoPassword";
    int traefikWebPort;

    public AasITExtension() {
    }

    public AasITExtension(boolean runLoadBalancer) {
        RUN_LOAD_BALANCER = runLoadBalancer;
    }

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (runningContainers.size() == 0) {
            traefikContainer = getTraefikContainer();
            Integer traefikHttpPort = null;
            Integer traefikHttpsPort = null;
            Integer traefikApiPort = null;
            if(RUN_LOAD_BALANCER) {
                traefikHttpPort = traefikContainer.getMappedPort(80);
                traefikHttpsPort = traefikContainer.getMappedPort(443);
                traefikApiPort = traefikContainer.getMappedPort(8080);
            }

            mqttBrokerContainer = getMqttBrokerContainer();
            mqttExporterContainer = getMqttExporterContainer();
            mqttGuiContainer = getMqttGuiContainer();
            mqttExporterContainer.setPortBindings(List.of("9234:9234"));

            aasRegistryContainer = getAasRegistryContainer();
            smRegistryContainer = getSmRegistryContainer();
            if(RUN_LOAD_BALANCER)
                aasEnvContainers = getAasEnvContainerList(traefikHttpPort);
            else
                aasEnvContainers = getAasEnvContainerList();
            aasEnvMongoDbContainer = getAasEnvMongoDbContainer();

            // Start AAS backend containers
            List<GenericContainer> containers = new ArrayList<>();
            if(RUN_LOAD_BALANCER) {
                containers.add(traefikContainer);
                containers.addAll(aasEnvContainers);
            } else {
                containers.add(aasEnvContainers.get(0));
            }
            containers.add(mqttBrokerContainer);
            containers.add(mqttGuiContainer);
            containers.add(mqttExporterContainer);
            containers.add(aasEnvMongoDbContainer);
            containers.add(aasRegistryContainer);
            containers.add(smRegistryContainer);
            startContainer(containers);

            if(RUN_LOAD_BALANCER) {
                runningContainers.add(traefikContainer);
                runningContainers.addAll(aasEnvContainers);
            } else {
                runningContainers.add(aasEnvContainers.get(0));
            }
            runningContainers.add(mqttBrokerContainer);
            runningContainers.add(mqttGuiContainer);
            runningContainers.add(mqttExporterContainer);
            runningContainers.add(aasEnvMongoDbContainer);
            runningContainers.add(aasRegistryContainer);
            runningContainers.add(smRegistryContainer);

            Integer aasRegistryPort = aasRegistryContainer.getMappedPort(8080);
            Integer submodelRegistryPort = smRegistryContainer.getMappedPort(8080);
            Integer aasEnvPort = aasEnvContainers.get(0).getMappedPort(8081);
            Integer aasEnvMongoDbPort = aasEnvMongoDbContainer.getMappedPort(27017);
            Integer mqttBrokerPort = mqttBrokerContainer.getMappedPort(1883);
            Integer mqttBrokerWsPort = mqttBrokerContainer.getMappedPort(9001);
            Integer mqttGuiPort = mqttGuiContainer.getMappedPort(80);

            // Start AAS GUI container
            aasGuiContainer = getAasGuiContainer(aasRegistryPort, submodelRegistryPort, aasEnvPort);
            aasGuiContainer.start();
            runningContainers.add(aasGuiContainer);

            var aasGuiPort = aasGuiContainer.getMappedPort(3000);

            System.setProperty("aas.broker.host", HOSTNAME);
            System.setProperty("aas.broker.port", mqttBrokerPort.toString());
            System.setProperty("aas.aas-registry.port", aasRegistryPort.toString());
            System.setProperty("aas.submodel-registry.port", submodelRegistryPort.toString());
            System.setProperty("aas.aas-registry.url", "http://" + HOSTNAME + ":" + aasRegistryPort);
            System.setProperty("aas.submodel-registry.url", "http://" + HOSTNAME + ":" + submodelRegistryPort);

            if (RUN_LOAD_BALANCER) {
                System.setProperty("aas.aas-repository.port", String.valueOf(traefikWebPort));
                System.setProperty("aas.aas-repository.path", aasEnvUrlPrefix);
                System.setProperty("aas.aas-repository.url", "http://" + HOSTNAME + ":" + traefikWebPort + aasEnvUrlPrefix);

                System.setProperty("aas.submodel-repository.port", String.valueOf(traefikWebPort));
                System.setProperty("aas.submodel-repository.path", aasEnvUrlPrefix);
                System.setProperty("aas.submodel-repository.url", "http://" + HOSTNAME + ":" + traefikWebPort + aasEnvUrlPrefix);
            } else {
                System.setProperty("aas.aas-repository.port", String.valueOf(aasEnvPort));
                System.setProperty("aas.aas-repository.url", "http://" + HOSTNAME + ":" + String.valueOf(aasEnvPort));

                System.setProperty("aas.submodel-repository.port", String.valueOf(aasEnvPort));
                System.setProperty("aas.submodel-repository.url", "http://" + HOSTNAME + ":" + String.valueOf(aasEnvPort));
            }

            if(traefikApiPort != null) {
                System.out.println("Traefik (http): http://" + HOSTNAME + ":" + traefikHttpPort);
                System.out.println("Traefik (https): http://" + HOSTNAME + ":" + traefikHttpsPort);
                System.out.println("Traefik Dashboard: http://" + HOSTNAME + ":" + traefikApiPort+ "/dashboard/");
            }
            System.out.println("AAS Registry: http://" + HOSTNAME + ":" + aasRegistryPort);
            System.out.println("Submodel Registry: http://" + HOSTNAME + ":" + submodelRegistryPort);
            if(RUN_LOAD_BALANCER) {
                System.out.println("AAS Environment: http://" + HOSTNAME + ":" + traefikWebPort + aasEnvUrlPrefix);
            } else {
                System.out.println("AAS Environment: http://" + HOSTNAME + ":" + aasEnvPort);
            }
            System.out.println("AAS Env Db: http://" + HOSTNAME + ":" + aasEnvMongoDbPort);
            System.out.println("MQTT Broker: tcp://" + HOSTNAME + ":" + mqttBrokerPort);
            System.out.println("MQTT WS Broker: ws://" + HOSTNAME + ":" + mqttBrokerWsPort);
            System.out.println("MQTT GUI: http://" + HOSTNAME + ":" + mqttGuiPort);
            System.out.println("AAS GUI: http://" + HOSTNAME + ":" + aasGuiPort);

            super.beforeAll(context);
        }
    }

    private GenericContainer getAasRegistryContainer() {
        return new GenericContainer<>(
                DockerImageName.parse("eclipsebasyx/aas-registry-log-mem:2.0.0-SNAPSHOT"))
                .withNetwork(containerNetwork)
                .withNetworkAliases("aas-registry")
                .withLabels(containerLabels)
                .dependsOn()
                .withExposedPorts(8080)
                .withEnv("BASYX_CORS_ALLOWEDORIGINS", "*")
                .withEnv("BASYX_CORS_ALLOWEDMETHODS", "GET,POST,PATCH,DELETE,PUT,OPTIONS,HEAD")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
    }

    private GenericContainer getSmRegistryContainer() {
        return new GenericContainer<>(
                DockerImageName.parse("eclipsebasyx/submodel-registry-log-mem:2.0.0-SNAPSHOT"))
                .withNetwork(containerNetwork)
                .withNetworkAliases("sm-registry")
                .withLabels(containerLabels)
                .dependsOn()
                .withExposedPorts(8080)
                .withEnv("BASYX_CORS_ALLOWEDORIGINS", "*")
                .withEnv("BASYX_CORS_ALLOWEDMETHODS", "GET,POST,PATCH,DELETE,PUT,OPTIONS,HEAD")
                .waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
    }

    private List<GenericContainer> getAasEnvContainerList(Integer traefikHttpPort) {
        String externalUrl = "http://" + HOSTNAME + ":" + traefikHttpPort + aasEnvUrlPrefix;
        return List.of(
                getAasEnvContainer(externalUrl),
                getAasEnvContainer(externalUrl),
                getAasEnvContainer(externalUrl),
                getAasEnvContainer(externalUrl),
                getAasEnvContainer(externalUrl)
        );
    }

    private List<GenericContainer> getAasEnvContainerList() {
        return List.of(
                getAasEnvContainer("")
        );
    }

    private GenericContainer getAasEnvContainer(String externalUrl) {
        int randomFreeHostPort;
        try {
            ServerSocket socket = new ServerSocket(0);
            randomFreeHostPort = socket.getLocalPort();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        if(externalUrl.equals("")) {
            externalUrl = "http://" + HOSTNAME + ":" + randomFreeHostPort;
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

//        return new FixedHostPortGenericContainer<>("eclipsebasyx/aas-environment:2.0.0-SNAPSHOT")
//                .withFixedExposedPort(randomFreeHostPort, 8081)
        return new GenericContainer<>(
                DockerImageName.parse("eclipsebasyx/aas-environment:2.0.0-SNAPSHOT"))
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
    }

    private GenericContainer getAasEnvMongoDbContainer() {
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

    private GenericContainer getAasGuiContainer(Integer aasRegistryPort, Integer submodelRegistryPort, Integer aasEnvPort) {
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

    private GenericContainer getMqttBrokerContainer() {
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

    private GenericContainer getMqttExporterContainer() {
        return new GenericContainer(
                DockerImageName.parse("jryberg/mosquitto-exporter:v0.7.4"))
                .withNetwork(containerNetwork)
                .withNetworkAliases("mqtt-exporter")
                .withLabels(containerLabels)
                .withCommand("--endpoint tcp://mqtt:1883")
                .withExposedPorts(9234)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
    }

    private GenericContainer getMqttGuiContainer() {
        return new GenericContainer(
                DockerImageName.parse("emqx/mqttx-web"))
                .withNetwork(containerNetwork)
                .withNetworkAliases("mqtt-gui")
                .withLabels(containerLabels)
                .withExposedPorts(80)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(CONTAINER_STARTUP_TIMEOUT));
    }

    private GenericContainer getTraefikContainer() throws IOException {
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
