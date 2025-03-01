package de.fhg.ipa.aas_transformer.clients.management;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import de.fhg.ipa.aas_transformer.clients.ApiClient;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Component
public class ManagementClient extends TransformerRestControllerApi {
    private static ObjectMapper objectMapper;
    private static WebClient webClient;
    static {
        objectMapper = new ObjectMapper();
        SimpleModule simpleModule = new SimpleModule();
        simpleModule.addSerializer(new SubmodelSerializer(Submodel.class));
        objectMapper.registerModule(simpleModule);

        // Create WebClient:
        ConnectionProvider provider = ConnectionProvider.builder("fixed")
                .maxConnections(500)
                .maxIdleTime(Duration.ofSeconds(20))
                .maxLifeTime(Duration.ofSeconds(60))
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .evictInBackground(Duration.ofSeconds(120)).build();

        HttpClient httpClient = HttpClient.create(provider);
        httpClient.warmup().block();

        var reactorClientHttpConnector = new ReactorClientHttpConnector(httpClient);
        webClient = WebClient.builder()
                .clientConnector(reactorClientHttpConnector)
                .build();
    }

    public ManagementClient(@Value("${aas_transformer.services.management.base-url}") String baseUrl) {
        super(new ApiClient(
                webClient,
                objectMapper,
                ApiClient.createDefaultDateFormat()
            ).setBasePath(baseUrl)
        );
    }
}
