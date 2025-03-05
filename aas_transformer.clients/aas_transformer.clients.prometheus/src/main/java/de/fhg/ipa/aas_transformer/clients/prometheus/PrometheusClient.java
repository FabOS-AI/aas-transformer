package de.fhg.ipa.aas_transformer.clients.prometheus;

import de.fhg.ipa.aas_transformer.clients.prometheus.model.AlertResponse;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

public class PrometheusClient {
    private final WebClient webClient;

    public PrometheusClient(String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public AlertResponse getAlerts() {
        return this.webClient
                .get()
                .uri("/api/v1/alerts")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(AlertResponse.class)
                .block();
    }
}
