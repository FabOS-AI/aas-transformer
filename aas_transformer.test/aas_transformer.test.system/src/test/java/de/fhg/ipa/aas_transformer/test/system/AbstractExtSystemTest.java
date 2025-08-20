package de.fhg.ipa.aas_transformer.test.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.fhg.ipa.aas_transformer.aas.*;
import de.fhg.ipa.aas_transformer.aas.serializer.SubmodelDescriptorSerializer;
import de.fhg.ipa.aas_transformer.aas.serializer.SubmodelElementSerializer;
import de.fhg.ipa.aas_transformer.aas.serializer.SubmodelSerializer;
import de.fhg.ipa.aas_transformer.clients.management.*;
import de.fhg.ipa.aas_transformer.aas.deserializer.SubmodelDescriptorDeserializer;
import de.fhg.ipa.aas_transformer.aas.deserializer.SubmodelDeserializer;
import de.fhg.ipa.aas_transformer.aas.deserializer.SubmodelElementDeserializer;
import de.fhg.ipa.aas_transformer.test.utils.GrafanaClient;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelDescriptor;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

public class AbstractExtSystemTest {

    //region Test Vars
    // IP addresses/hostnames:
    static String host = "aas-transformer.local";

    // Subdomains
    static String transformerManagementSubdomain = "management";
    static String transformerExecutorSubdomain = "executor";
    static String transformerListenerSubdomain = "listener";
    static String grafanaSubdomain = "grafana";

    // Service Ports:
    static String transformerManagementPort = "80";
    static String transformerExecutorPort = "80";
    static String transformerListenerPort = "80";
    static String grafanaPort = "80";
    static String aasRegistryPort = "80";
    static String aasRegistryPath = "/shell-registry";
    static String aasRepositoryPort = "80";
    static String aasRepositoryPath = "";
    static String smRegistryPort = "80";
    static String smRegistryPath = "/sm-registry";
    static String smRepositoryPort = "80";
    static String smRepositoryPath = "";

    // Transformer Service Urls:
    static String transformerManagementUrl = "http://" + transformerManagementSubdomain + "." + host +":" + transformerManagementPort;
    static String transformerExecutorUrl = "http://" + transformerExecutorSubdomain + "." + host + ":" + transformerExecutorPort;
    static String transformerListenerUrl = "http://" + transformerListenerSubdomain + "." + host + ":" + transformerListenerPort;
    static String grafanaUrl = "http://"+ grafanaSubdomain + "." + host +":" + grafanaPort;

    // AAS Service Urls:
    static String aasRegistryUrl = "http://"+host+":"+aasRegistryPort+aasRegistryPath;
    static String aasRepoUrl = "http://"+host+":"+aasRepositoryPort+aasRepositoryPath;
    static String smRegistryUrl = "http://"+host+":"+smRegistryPort+smRegistryPath;
    static String smRepoUrl = "http://"+host+":"+smRepositoryPort+smRepositoryPath;

    // Service Clients:
    protected static ManagementClient managementClient;
    protected static WebClient executorWebclient;
    protected static WebClient listenerWebclient;
    protected static MetricsClient metricsClient;
    protected static RedisControllerClient redisControllerClient;
    protected static ScalingClient scalingClient;
    protected static AasRegistry aasRegistry;
    protected static AasRepository aasRepository;
    protected static SubmodelRegistry smRegistry;
    protected static SubmodelRepository smRepository;
    protected static GrafanaClient grafanaClient;

    // Credentials
    static String grafanaUsername = "admin";
    static String grafanaPassword = "admin";

    static {
        managementClient = new ManagementClient(transformerManagementUrl);
        executorWebclient = getWebclient(transformerExecutorUrl);
        listenerWebclient = getWebclient(transformerListenerUrl);
        metricsClient = new MetricsClient(transformerManagementUrl);
        redisControllerClient = new RedisControllerClient(transformerManagementUrl);
        scalingClient = new ScalingClient(transformerManagementUrl);
        grafanaClient = new GrafanaClient(grafanaUrl, grafanaUsername, grafanaPassword);

        aasRegistry = new AasRegistry(aasRegistryUrl, aasRepoUrl);
        aasRepository = new AasRepository(aasRepoUrl);
        smRegistry = new SubmodelRegistry(smRegistryUrl, smRepoUrl);
        smRepository = new SubmodelRepository(smRepoUrl);

    }
    // endregion


    private static WebClient getWebclient(String baseUrl) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        // SubmodelDescriptors:
        simpleModule.addSerializer(new SubmodelDescriptorSerializer(SubmodelDescriptor.class));
        simpleModule.addDeserializer(SubmodelDescriptor.class, new SubmodelDescriptorDeserializer());
        // Submodels:
        simpleModule.addSerializer(Submodel.class, new SubmodelSerializer());
        simpleModule.addDeserializer(Submodel.class, new SubmodelDeserializer());
        // SubmodelElements:
        simpleModule.addSerializer(new SubmodelElementSerializer(SubmodelElement.class));
        simpleModule.addDeserializer(SubmodelElement.class, new SubmodelElementDeserializer());
        objectMapper.registerModule(simpleModule);

        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
                }).build();

        return WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(strategies)
                .build();
    }
}
