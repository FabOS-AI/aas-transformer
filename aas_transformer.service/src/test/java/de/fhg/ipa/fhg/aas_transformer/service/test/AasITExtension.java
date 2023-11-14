package de.fhg.ipa.fhg.aas_transformer.service.test;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;

public class AasITExtension implements BeforeAllCallback, AfterAllCallback {

    //region DOCKER SERVICE INFO
    private final static String REGISTRY_SERVICE_NAME = "aas-registry";
    private final static int REGISTRY_SERVICE_PORT = 4000;
    private final static String SERVER_SERVICE_NAME = "aas-server";
    private final static int SERVER_SERVICE_PORT = 4001;
    private final static String BROKER_SERVICE_NAME = "aas-mqtt-broker";
    private final static int BROKER_SERVICE_PORT = 1883;
    //endregion

    private final String dockerComposeFilePath = "src/test/docker/aas/docker-compose.yml";

    private DockerComposeContainer dockerCompose;

    @Override
    public void beforeAll(ExtensionContext context) throws Exception {
        if (dockerCompose == null) {
            dockerCompose = new DockerComposeContainer(new File(dockerComposeFilePath))
                    .withBuild(true)
                    .withExposedService(
                            REGISTRY_SERVICE_NAME,
                            REGISTRY_SERVICE_PORT,
                            Wait.forHttp("/registry/api/v1/registry")
                    )
                    .withExposedService(
                            SERVER_SERVICE_NAME,
                            SERVER_SERVICE_PORT,
                            Wait.forHttp("/aasServer/shells")
                    )
                    .withExposedService(
                            BROKER_SERVICE_NAME,
                            BROKER_SERVICE_PORT,
                            Wait.forListeningPort()
                    );

            dockerCompose.start();

            System.setProperty("aas.broker.port", dockerCompose.getServicePort(BROKER_SERVICE_NAME, BROKER_SERVICE_PORT).toString());
            System.setProperty("aas.registry.port", dockerCompose.getServicePort(REGISTRY_SERVICE_NAME, REGISTRY_SERVICE_PORT).toString());
            System.setProperty("aas.server.port", dockerCompose.getServicePort(SERVER_SERVICE_NAME, SERVER_SERVICE_PORT).toString());
        }
    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {
        if (dockerCompose != null && context.getParent().get().getParent().isEmpty()) {
            dockerCompose.stop();
            dockerCompose = null;
        }
    }

}
