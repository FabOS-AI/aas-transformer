package de.fhg.ipa.aas_transformer.test.utils.extentions;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AasTransformerExtension extends AbstractExtension implements BeforeAllCallback, AfterAllCallback {
    private boolean runManagement = true;
    private boolean runListener = true;
    private boolean runExecutor = true;

    public int listenerInstanceCount = 1;
    public int executorInstanceCount = 1;

    protected Network containerNetwork = Network.newNetwork();
    protected GenericContainer aasTransformerManagement;
    protected GenericContainer aasTransformerListener;
    protected List<GenericContainer> aasTransformerExecutors = new ArrayList<>();
    protected List<GenericContainer> runningContainers = new ArrayList<>();

    protected String dockerImageVersion = "2.0.0-SNAPSHOT";

    Map<String, String> containerLabels = Map.of(
            "de.fhg.ipa.aas-transformer.container-group","transformer"
    );

    // Application Properties:

    // REDIS:
    String redisPort;
    String redisUsername;
    String redisPassword;

    // DB:
    String databasePort;
    String databaseSchema;
    String databaseUsername;
    String databasePassword;

    // AAS:
    String aasBrokerPort;
    String aasRegistryPort;
    String aasRepoPort;
    String aasRepoPath = "";
    String aasSubmodelRegistryPort;
    String aasSubmodelRepoPort;
    String aasSubmodelRepoPath = "";

    public AasTransformerExtension() {}

    public AasTransformerExtension(
            boolean runManagement,
            boolean runListener,
            boolean runExecutor
    ) {
        this.runManagement = runManagement;
        this.runListener = runListener;
        this.runExecutor = runExecutor;
    }

    public AasTransformerExtension(
            int listenerInstanceCount,
            int executorInstanceCount
    ) {
        this.listenerInstanceCount = listenerInstanceCount;
        this.executorInstanceCount = executorInstanceCount;
    }

    public void resetExecutorContainer(int desiredContainerCount, PrometheusExtension prometheusExtension) {
        Instant start = Instant.now();
        boolean changedContainerCount = resetExecutorContainer(desiredContainerCount);
        Instant end = Instant.now();

        if (changedContainerCount) {
            prometheusExtension.createAnnotation(
                    start,
                    end,
                    List.of("test", "scale_container"),
                    "Scale Container count"
            );
        }


    }

    public boolean resetExecutorContainer(int desiredContainerCount) {

        int currentContainerCount = aasTransformerExecutors.size();
        int diff = desiredContainerCount - currentContainerCount;

        if(diff > 0) {
            System.out.println("Resetting Executor Container Count to: " + desiredContainerCount);
            System.out.println("Adding " + diff + " Executor Containers");
            List<GenericContainer> containers = new ArrayList<>();
            for(int i = 0; i < diff; i++) {
                GenericContainer container = getExecutorContainer();
                containers.add(container);

                aasTransformerExecutors.add( container );
            }
            startContainer(containers);
        } else if (diff < 0) {
            System.out.println("Resetting Executor Container Count to: " + desiredContainerCount);
            System.out.println("Removing " + Math.abs(diff) + " Executor Containers");
            for(int i = 0; i < diff; i++) {
                GenericContainer container = aasTransformerExecutors.get(0);
                container.stop();

                aasTransformerExecutors.remove( container );
            }
        } else {
            System.out.println("Executor Container Count is already at desired count: " + desiredContainerCount);
        }

        if(diff != 0) {
            return true; // Container count was changed
        } else {
            return false; // Container count was not changed
        }
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        lookupApplicationProperties();

        aasTransformerManagement = getManagementContainer();

        aasTransformerManagement.setPortBindings(List.of("4010:4010"));

        if(runManagement) {
            aasTransformerManagement.start();
            runningContainers.add(aasTransformerManagement);
        }

        System.setProperty(
                "aas_transformer.services.management.port",
                String.valueOf(aasTransformerManagement.getMappedPort(4010))
        );

        aasTransformerListener = getListenerContainer();

        for(int i = 0; i < executorInstanceCount; i++) {
            aasTransformerExecutors.add( getExecutorContainer() );
        }

        List<Integer> aasTransformerExecutorPorts = new ArrayList<>();
        Integer aasTransformerListenerPort;

        if(runListener) {
            aasTransformerListener.start();
            runningContainers.add(aasTransformerListener);
            aasTransformerListenerPort = aasTransformerListener.getMappedPort(4020);
        }
        if(runExecutor) {
            aasTransformerExecutors.forEach(c -> c.start());
            runningContainers.addAll(aasTransformerExecutors);
            aasTransformerExecutors.forEach(c -> {
                aasTransformerExecutorPorts.add(c.getMappedPort(4030));
            });
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        super.afterAll(context);
    }

    private void lookupApplicationProperties() {
        // Application Properties:
        // REDIS:
        redisPort = System.getProperty("spring.data.redis.port");
        redisUsername = System.getProperty("spring.data.redis.username");
        redisPassword = System.getProperty("spring.data.redis.password");

        // DB:
        databasePort = System.getProperty("database.port");
        databaseSchema = System.getProperty("database.schema");
        databaseUsername = System.getProperty("database.username");
        databasePassword = System.getProperty("database.password");

        // AAS:
        aasBrokerPort = System.getProperty("aas.broker.port");
        aasRegistryPort = System.getProperty("aas.aas-registry.port");
        aasRepoPort = System.getProperty("aas.aas-repository.port");
        aasRepoPath = System.getProperty("aas.aas-repository.path");
        aasSubmodelRegistryPort = System.getProperty("aas.submodel-registry.port");
        aasSubmodelRepoPort = System.getProperty("aas.submodel-repository.port");
        aasSubmodelRepoPath = System.getProperty("aas.submodel-repository.path");
    }

    private GenericContainer getManagementContainer() {
        return new GenericContainer<>(
                DockerImageName.parse("ghcr.io/fabos-ai/aas-transformer/management:" + dockerImageVersion))
                .withLabels(containerLabels)
                .withNetwork(containerNetwork)
                .withNetworkAliases("aas-transformer-management")
                .dependsOn()
                .withExposedPorts(4010)
                .withEnv("SPRING_DATA_REDIS_HOST", "host.docker.internal")
                .withEnv("SPRING_DATA_REDIS_PORT", redisPort)
                .withEnv("SPRING_DATA_REDIS_USERNAME", redisUsername)
                .withEnv("SPRING_DATA_REDIS_PASSWORD", redisPassword)
                .withEnv("DATABASE_HOST", "host.docker.internal")
                .withEnv("DATABASE_PORT", databasePort)
                .withEnv("DATABASE_SCHEMA", databaseSchema)
                .withEnv("DATABASE_USERNAME", databaseUsername)
                .withEnv("DATABASE_PASSWORD", databasePassword)
                .withEnv("AAS_BROKER_HOST", "host.docker.internal")
                .withEnv("AAS_BROKER_PORT", aasBrokerPort)
                .withEnv("AAS_AAS_REGISTRY_HOST", "host.docker.internal")
                .withEnv("AAS_AAS_REGISTRY_PORT", aasRegistryPort)
                .withEnv("AAS_AAS_REPOSITORY_HOST", "host.docker.internal")
                .withEnv("AAS_AAS_REPOSITORY_PORT", aasRepoPort)
                .withEnv("AAS_AAS_REPOSITORY_PATH", aasRepoPath)
                .withEnv("AAS_SUBMODEL_REGISTRY_HOST", "host.docker.internal")
                .withEnv("AAS_SUBMODEL_REGISTRY_PORT", aasSubmodelRegistryPort)
                .withEnv("AAS_SUBMODEL_REPOSITORY_HOST", "host.docker.internal")
                .withEnv("AAS_SUBMODEL_REPOSITORY_PORT", aasSubmodelRepoPort)
                .withEnv("AAS_SUBMODEL_REPOSITORY_PATH", aasSubmodelRepoPath)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)));
    }

    private GenericContainer getListenerContainer() {
        String aasTransformerManagementPort = System.getProperty("aas_transformer.services.management.port");

        return new GenericContainer<>(
                DockerImageName.parse("ghcr.io/fabos-ai/aas-transformer/listener:" + dockerImageVersion))
                .withLabels(containerLabels)
                .withNetwork(containerNetwork)
                .withNetworkAliases("aas-transformer-listener")
                .dependsOn()
                .withExposedPorts(4020)
                .withEnv("SPRING_DATA_REDIS_HOST", "host.docker.internal")
                .withEnv("SPRING_DATA_REDIS_PORT", redisPort)
                .withEnv("SPRING_DATA_REDIS_USERNAME", redisUsername)
                .withEnv("SPRING_DATA_REDIS_PASSWORD", redisPassword)
                .withEnv("AAS_TRANSFORMER_SERVICES_MANAGEMENT_HOST", "host.docker.internal")
                .withEnv("AAS_TRANSFORMER_SERVICES_MANAGEMENT_PORT", String.valueOf(aasTransformerManagementPort))
                .withEnv("AAS_BROKER_HOST", "host.docker.internal")
                .withEnv("AAS_BROKER_PORT", aasBrokerPort)
                .withEnv("AAS_AAS_REGISTRY_HOST", "host.docker.internal")
                .withEnv("AAS_AAS_REGISTRY_PORT", aasRegistryPort)
                .withEnv("AAS_AAS_REPOSITORY_HOST", "host.docker.internal")
                .withEnv("AAS_AAS_REPOSITORY_PORT", aasRepoPort)
                .withEnv("AAS_AAS_REPOSITORY_PATH", aasRepoPath)
                .withEnv("AAS_SUBMODEL_REGISTRY_HOST", "host.docker.internal")
                .withEnv("AAS_SUBMODEL_REGISTRY_PORT", aasSubmodelRegistryPort)
                .withEnv("AAS_SUBMODEL_REPOSITORY_HOST", "host.docker.internal")
                .withEnv("AAS_SUBMODEL_REPOSITORY_PORT", aasSubmodelRepoPort)
                .withEnv("AAS_SUBMODEL_REPOSITORY_PATH", aasSubmodelRepoPath);
    }

    private GenericContainer getExecutorContainer() {
        String aasTransformerManagementPort = System.getProperty("aas_transformer.services.management.port");

        return new GenericContainer<>(
                DockerImageName.parse("ghcr.io/fabos-ai/aas-transformer/executor:" + dockerImageVersion))
                .withLabels(containerLabels)
                .withNetwork(containerNetwork)
                .withNetworkAliases("aas-transformer-executor")
                .dependsOn()
                .withExposedPorts(4030)
                .withEnv("SPRING_DATA_REDIS_HOST", "host.docker.internal")
                .withEnv("SPRING_DATA_REDIS_PORT", redisPort)
                .withEnv("SPRING_DATA_REDIS_USERNAME", redisUsername)
                .withEnv("SPRING_DATA_REDIS_PASSWORD", redisPassword)
                .withEnv("AAS_TRANSFORMER_SERVICES_MANAGEMENT_HOST", "host.docker.internal")
                .withEnv("AAS_TRANSFORMER_SERVICES_MANAGEMENT_PORT", aasTransformerManagementPort)
                .withEnv("AAS_BROKER_HOST", "host.docker.internal")
                .withEnv("AAS_BROKER_PORT", aasBrokerPort)
                .withEnv("AAS_AAS_REGISTRY_HOST", "host.docker.internal")
                .withEnv("AAS_AAS_REGISTRY_PORT", aasRegistryPort)
                .withEnv("AAS_AAS_REPOSITORY_HOST", "host.docker.internal")
                .withEnv("AAS_AAS_REPOSITORY_PORT", aasRepoPort)
                .withEnv("AAS_AAS_REPOSITORY_PATH", aasRepoPath)
                .withEnv("AAS_SUBMODEL_REGISTRY_HOST", "host.docker.internal")
                .withEnv("AAS_SUBMODEL_REGISTRY_PORT", aasSubmodelRegistryPort)
                .withEnv("AAS_SUBMODEL_REPOSITORY_HOST", "host.docker.internal")
                .withEnv("AAS_SUBMODEL_REPOSITORY_PORT", aasSubmodelRepoPort)
                .withEnv("AAS_SUBMODEL_REPOSITORY_PATH", aasSubmodelRepoPath)
                .waitingFor(Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(3)));
    }
}
