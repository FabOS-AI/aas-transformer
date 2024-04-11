package de.fhg.ipa.fhg.aas_transformer.service.test;

import de.fhg.ipa.aas_transformer.service.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.service.aas.AasRepository;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.actions.TransformerActionServiceFactory;
import de.fhg.ipa.aas_transformer.service.TransformerHandler;
import de.fhg.ipa.aas_transformer.service.TransformerServiceFactory;
import de.fhg.ipa.aas_transformer.service.templating.SubmodelTemplateFunctions;
import de.fhg.ipa.aas_transformer.service.templating.AppTemplateFunctions;
import de.fhg.ipa.aas_transformer.service.templating.TemplateRenderer;
import de.fhg.ipa.aas_transformer.service.templating.UUIDTemplateFunctions;
import org.eclipse.digitaltwin.aas4j.v3.dataformat.core.DeserializationException;
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

    private static final String AAS_REGISTRY_DOCKER_IMAGE_NAME = "eclipsebasyx/aas-registry-log-mem:2.0.0-SNAPSHOT";
    private static final int AAS_REGISTRY_PRIVATE_PORT = 8080;
    private int aasRegistryPublicPort;

    private static final String SUBMODEL_REGISTRY_DOCKER_IMAGE_NAME = "eclipsebasyx/submodel-registry-log-mem:2.0.0-SNAPSHOT";
    private static final int SUBMODEL_REGISTRY_PRIVATE_PORT = 8080;
    private int submodelRegistryPublicPort;

    public static String AAS_ENV_DOCKER_IMAGE_NAME = "eclipsebasyx/aas-environment:2.0.0-SNAPSHOT";
    private static final int AAS_ENV_PRIVATE_PORT = 8081;
    private int submodelRepositoryPublicPort;
    private int aasRepositoryPublicPort;

    protected AasRegistry aasRegistry;

    protected AasRepository aasRepository;

    protected SubmodelRepository submodelRepository;

    protected SubmodelRegistry submodelRegistry;

    protected TransformerHandler transformerHandler;

    protected TransformerServiceFactory transformerServiceFactory;

    public String getAasRegistryUrl() {
        return "http://localhost:" + aasRegistryPublicPort;
    }

    public String getAasRepositoryUrl() {
        return "http://localhost:"+ aasRepositoryPublicPort;
    }

    public String getSubmodelRegistryUrl() {
        return "http://localhost:" + submodelRegistryPublicPort;
    }

    public String getSubmodelRepositoryUrl() {
        return "http://localhost:"+ submodelRepositoryPublicPort;
    }

    protected static final Network containerNetwork = Network.newNetwork();

    @Container
    protected static GenericContainer smRegistryContainer;

    @Container
    protected static GenericContainer aasEnvContainer;

    static {
        smRegistryContainer = new GenericContainer<>(
                DockerImageName.parse(AbstractIT.SUBMODEL_REGISTRY_DOCKER_IMAGE_NAME))
                .withNetwork(containerNetwork)
                .withNetworkAliases("sm-registry")
                .dependsOn()
                .withExposedPorts(AbstractIT.SUBMODEL_REGISTRY_PRIVATE_PORT)
                .waitingFor(Wait.forHttp("/swagger-ui.html"));
        smRegistryContainer.start();

        try {
            var socket = new ServerSocket(0);
            var randomFreeHostPort = socket.getLocalPort();
            socket.close();

            aasEnvContainer = new FixedHostPortGenericContainer(AbstractIT.AAS_ENV_DOCKER_IMAGE_NAME)
                    .withFixedExposedPort(randomFreeHostPort, AbstractIT.AAS_ENV_PRIVATE_PORT)
                    .withExposedPorts(AbstractIT.AAS_ENV_PRIVATE_PORT)
                    .withNetwork(containerNetwork)
                    .dependsOn(smRegistryContainer)
                    .withEnv("BASYX_SUBMODELREPOSITORY_FEATURE_REGISTRYINTEGRATION", "http://sm-registry:8080")
                    .withEnv("BASYX_EXTERNALURL", "http://localhost:" + randomFreeHostPort);
            aasEnvContainer.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeAll
    public void beforeAll() throws IOException, DeserializationException {
        this.submodelRegistryPublicPort = smRegistryContainer.getFirstMappedPort();
        this.submodelRepositoryPublicPort = aasEnvContainer.getFirstMappedPort();

        this.submodelRegistry = new SubmodelRegistry(this.getSubmodelRegistryUrl(), this.getSubmodelRepositoryUrl());
        this.submodelRepository = new SubmodelRepository(this.getSubmodelRepositoryUrl());
        var transformerActionServiceFactory = new TransformerActionServiceFactory(this.getTemplateRenderer(), submodelRegistry, submodelRepository);
        this.transformerServiceFactory = new TransformerServiceFactory(
                this.getTemplateRenderer(),
                aasRegistry, aasRepository,
                submodelRegistry, submodelRepository,
                transformerActionServiceFactory);
    }

    protected void assertSubmodelExists(String submodelId) {
        var submodel = submodelRepository.getSubmodel(submodelId);
        assertThat(submodel).isNotNull();
    }

    private TemplateRenderer getTemplateRenderer() {
        Map<String, Object> propertiesMap = Map.of("aas.server.url", "http://localhost:4001/aasServer");

        var aasTemplateFunctions = new SubmodelTemplateFunctions(this.submodelRegistry, this.submodelRepository);

        var environment = new StandardEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        propertySources.addFirst(new MapPropertySource("propertiesMap", propertiesMap));
        var appTemplateFunctions = new AppTemplateFunctions(environment);

        var uuidTemplateFunctions = new UUIDTemplateFunctions();
        return new TemplateRenderer(aasTemplateFunctions, appTemplateFunctions, uuidTemplateFunctions);
    }
}
