package de.fhg.ipa.aas_transformer.clients.alertmanager;

import de.fhg.ipa.aas_transformer.model.alertmanager.Alert;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

public class AlertManagerClient {
    private final WebClient webClient;

    public AlertManagerClient(String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public List<Alert> getAlerts() {
        return this.webClient
                .get()
                .uri("/api/v2/alerts")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Alert>>() {})
                .block();
    }
}
