package de.fhg.ipa.aas_transformer.test.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.clients.management.*;
import de.fhg.ipa.aas_transformer.clients.redis.SubmodelDeserializer;
import de.fhg.ipa.aas_transformer.clients.redis.SubmodelElementDeserializer;
import de.fhg.ipa.aas_transformer.test.system.performance.model.AggregatedTestResult;
import de.fhg.ipa.aas_transformer.test.system.performance.model.TestResult;
import de.fhg.ipa.aas_transformer.test.utils.GrafanaClient;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.SubmodelElement;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;

public class AbstractExtSystemTest {

    //region Test Vars
    // IP addresses/hostnames:
    String host = "aas-transformer.local";

    // Subdomains
    String transformerManagementSubdomain = "management";
    String transformerExecutorSubdomain = "executor";
    String transformerListenerSubdomain = "listener";
    String grafanaSubdomain = "grafana";

    // Service Ports:
    String transformerManagementPort = "80";
    String transformerExecutorPort = "80";
    String transformerListenerPort = "80";
    String grafanaPort = "80";
    String aasRegistryPort = "80";
    String aasRegistryPath = "/shell-registry";
    String aasRepositoryPort = "80";
    String aasRepositoryPath = "";
    String smRegistryPort = "80";
    String smRegistryPath = "/sm-registry";
    String smRepositoryPort = "80";
    String smRepositoryPath = "";

    // Transformer Service Urls:
    String transformerManagementUrl = "http://" + transformerManagementSubdomain + "." + host +":" + transformerManagementPort;
    String transformerExecutorUrl = "http://" + transformerExecutorSubdomain + "." + host + ":" + transformerExecutorPort;
    String transformerListenerUrl = "http://" + transformerListenerSubdomain + "." + host + ":" + transformerListenerPort;
    String grafanaUrl = "http://"+ grafanaSubdomain + "." + host +":" + grafanaPort;

    // AAS Service Urls:
    String aasRegistryUrl = "http://"+host+":"+aasRegistryPort+aasRegistryPath;
    String aasRepoUrl = "http://"+host+":"+aasRepositoryPort+aasRepositoryPath;
    String smRegistryUrl = "http://"+host+":"+smRegistryPort+smRegistryPath;
    String smRepoUrl = "http://"+host+":"+smRepositoryPort+smRepositoryPath;

    // Service Clients:
    protected static ManagementClient managementClient;
    protected static WebClient executorWebclient;
    protected static WebClient listenerWebclient;
    protected static MetricsClient metricsClient;
    protected static JobsClient jobsClient;
    protected static ScalingClient scalingClient;
    protected static AasRegistry aasRegistry;
    protected static AasRepository aasRepository;
    protected static SubmodelRegistry smRegistry;
    protected static SubmodelRepository smRepository;
    protected static GrafanaClient grafanaClient;

    // Credentials
    static String grafanaUsername = "admin";
    static String grafanaPassword = "admin";

    // endregion


    public AbstractExtSystemTest() {
        managementClient = new ManagementClient(transformerManagementUrl);
        executorWebclient = getWebclient(transformerExecutorUrl);
        listenerWebclient = getWebclient(transformerListenerUrl);
        metricsClient = new MetricsClient(transformerManagementUrl);
        jobsClient = new JobsClient(transformerManagementUrl);
        scalingClient = new ScalingClient(transformerManagementUrl);
        grafanaClient = new GrafanaClient(grafanaUrl, grafanaUsername, grafanaPassword);

        aasRegistry = new AasRegistry(aasRegistryUrl, aasRepoUrl);
        aasRepository = new AasRepository(aasRepoUrl);
        smRegistry = new SubmodelRegistry(smRegistryUrl, smRepoUrl);
        smRepository = new SubmodelRepository(smRepoUrl);
    }

    private WebClient getWebclient(String baseUrl) {
        ObjectMapper objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        // Submodels:
        simpleModule.addSerializer(new SubmodelSerializer(Submodel.class));
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
