package de.fhg.ipa.aas_transformer.clients.alertmanager;

import de.fhg.ipa.aas_transformer.model.alertmanager.Alert;
import org.junit.jupiter.api.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Disabled
public class AlertManagerClientTest {
    private static final String ALERTMANAGER_URL = "http://alertmanager.aas-transformer.local";
    private AlertManagerClient alertManagerClient = new AlertManagerClient(ALERTMANAGER_URL);

    @Test
    @Order(10)
    public void testGetAlerts() {
        List<Alert> alerts = alertManagerClient.getAlerts();

        assertEquals(alerts.size(), 0);
    }
}
