package de.fhg.ipa.aas_transformer.clients.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.fhg.ipa.aas_transformer.aas.serializer.SubmodelSerializer;
import de.fhg.ipa.aas_transformer.clients.ApiClient;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

@Component
public class ManagementClient extends TransformerRestControllerApi {
    private static ObjectMapper objectMapper;
    private static WebClient webClient;
    private static ApiClient apiClient;
    static {
        objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(Submodel.class, new SubmodelSerializer());
        objectMapper.registerModule(simpleModule);

        // Create WebClient:
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
//                .maxConnections(500)
//                .maxIdleTime(Duration.ofSeconds(20))
//                .maxLifeTime(Duration.ofSeconds(60))
//                .pendingAcquireTimeout(Duration.ofSeconds(60))
//                .evictInBackground(Duration.ofSeconds(120))
                .build();

        HttpClient httpClient = HttpClient.create(provider);
        httpClient.warmup().block();

        var reactorClientHttpConnector = new ReactorClientHttpConnector(httpClient);

        ExchangeStrategies strategies = ExchangeStrategies
                .builder()
                .codecs(clientDefaultCodecsConfigurer -> {
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonEncoder(new Jackson2JsonEncoder(objectMapper, MediaType.APPLICATION_JSON));
                    clientDefaultCodecsConfigurer.defaultCodecs().jackson2JsonDecoder(new Jackson2JsonDecoder(objectMapper, MediaType.APPLICATION_JSON));
                }).build();

        webClient = WebClient.builder()
                .clientConnector(reactorClientHttpConnector)
                .exchangeStrategies(strategies)
                .build();

        apiClient = new ApiClient(webClient);
    }

    public ManagementClient(@Value("${aas_transformer.services.management.base-url}") String baseUrl) {
        super(apiClient.setBasePath(baseUrl));
    }
}
