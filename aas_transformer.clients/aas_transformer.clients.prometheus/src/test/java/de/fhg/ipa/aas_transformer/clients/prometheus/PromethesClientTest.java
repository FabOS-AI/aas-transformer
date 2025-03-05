package de.fhg.ipa.aas_transformer.clients.prometheus;

import de.fhg.ipa.aas_transformer.clients.prometheus.model.AlertResponse;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class PromethesClientTest {
    private static final String PROMETHEUS_URL = "http://aas-transformer.local/prometheus";
    private PrometheusClient prometheusClient = new PrometheusClient(PROMETHEUS_URL);

    @Test
    @Order(10)
    public void testGetAlerts() {
        AlertResponse alertResponse = prometheusClient.getAlerts();

        assertEquals(alertResponse.getData().getAlerts().size(), 0);
    }
}
