package de.fhg.ipa.fhg.aas_transformer.service.test;

import de.fhg.ipa.aas_transformer.service.actions.TransformerActionServiceFactory;
import de.fhg.ipa.aas_transformer.service.TransformerHandler;
import de.fhg.ipa.aas_transformer.service.TransformerServiceFactory;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.templating.AASTemplateFunctions;
import de.fhg.ipa.aas_transformer.service.templating.AppTemplateFunctions;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import de.fhg.ipa.aas_transformer.service.templating.UUIDTemplateFunctions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
public abstract class AbstractIT {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractIT.class);

    private static final String AAS_REGISTRY_DOCKER_IMAGE_NAME = "eclipsebasyx/aas-registry:1.4.0";
    private static final int AAS_REGISTRY_PRIVATE_PORT = 4000;
    private int aasRegistryPublicPort;

    public static String AAS_SERVER_DOCKER_IMAGE_NAME = "eclipsebasyx/aas-server:1.4.0";
    private static final int AAS_SERVER_PRIVATE_PORT = 4001;
    private int aasServerPublicPort;

    protected AASManager aasManager;

    protected AASRegistry aasRegistry;

    protected TransformerHandler transformerHandler;

    protected TransformerServiceFactory transformerServiceFactory;

    public String getAASRegistryUrl() {
        return "http://localhost:" + aasRegistryPublicPort + "/registry";
    }

    public String getAASServerUrl() {
        return "http://localhost:"+ aasServerPublicPort +"/aasServer";
    }

    protected static final Network containerNetwork = Network.newNetwork();

    @Container
    protected static GenericContainer aasRegistryContainer;

    @Container
    protected static GenericContainer aasServerContainer;

    static {
        aasRegistryContainer = new GenericContainer<>(
                DockerImageName.parse(AbstractIT.AAS_REGISTRY_DOCKER_IMAGE_NAME))
                .withNetwork(containerNetwork)
                .withNetworkAliases("aas-registry")
                .dependsOn()
                .withExposedPorts(AbstractIT.AAS_REGISTRY_PRIVATE_PORT)
                .waitingFor(Wait.forHttp("/registry/api/v1/registry"));
        aasRegistryContainer.start();

        try {
            var socket = new ServerSocket(0);
            var randomFreeHostPort = socket.getLocalPort();
            socket.close();

            aasServerContainer = new FixedHostPortGenericContainer(AbstractIT.AAS_SERVER_DOCKER_IMAGE_NAME)
                    .withFixedExposedPort(randomFreeHostPort, AbstractIT.AAS_SERVER_PRIVATE_PORT)
                    .withExposedPorts(AbstractIT.AAS_SERVER_PRIVATE_PORT)
                    .withNetwork(containerNetwork)
                    .dependsOn(aasRegistryContainer)
                    .withEnv("basyxaas_registry_path", "http://aas-registry:4000/registry/api/v1/registry")
                    .withEnv("basyxaas_registry_host", "http://localhost:" + randomFreeHostPort + "/aasServer");
            aasServerContainer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    public void beforeAll() throws IOException {
        this.aasRegistryPublicPort = aasRegistryContainer.getFirstMappedPort();
        this.aasServerPublicPort = aasServerContainer.getFirstMappedPort();

        this.aasRegistry = new AASRegistry(this.getAASRegistryUrl());
        this.aasManager = new AASManager(this.getAASServerUrl(), this.aasRegistry);
        var transformerActionServiceFactory = new TransformerActionServiceFactory(this.getTemplateRenderer(), aasManager);
        this.transformerServiceFactory = new TransformerServiceFactory(this.getTemplateRenderer(), aasRegistry, aasManager, transformerActionServiceFactory);
    }

    protected void assertSubmodelExists(String submodelId) {
        var submodels = aasManager.retrieveSubmodels(GenericTestConfig.AAS.getIdentification());
        assertThat(submodels).containsKey(submodelId);
    }

    private TemplateRenderer getTemplateRenderer() {
        Map<String, Object> propertiesMap = Map.of("aas.server.url", "http://localhost:4001/aasServer");

        var aasTemplateFunctions = new AASTemplateFunctions(this.aasRegistry, this.aasManager);

        var environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource("propertiesMap", propertiesMap));
        var appTemplateFunctions = new AppTemplateFunctions(environment);

        var uuidTemplateFunctions = new UUIDTemplateFunctions();
        return new TemplateRenderer(aasTemplateFunctions, appTemplateFunctions, uuidTemplateFunctions);
    }
}
