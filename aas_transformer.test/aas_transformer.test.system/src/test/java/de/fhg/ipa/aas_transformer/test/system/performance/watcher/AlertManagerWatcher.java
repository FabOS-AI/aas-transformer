package de.fhg.ipa.aas_transformer.test.system.performance.watcher;

import de.fhg.ipa.aas_transformer.clients.alertmanager.AlertManagerClient;
import de.fhg.ipa.aas_transformer.clients.alertmanager.model.Alert;
import de.fhg.ipa.aas_transformer.test.utils.GrafanaClient;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

public class AlertManagerWatcher implements Runnable {
    Thread thread = new Thread(this);
    boolean stopped = false;
    AlertManagerClient alertManagerClient;
    GrafanaClient grafanaClient;
    Map<String, OffsetDateTime> latestFiredAlerts = new HashMap<>();

    public AlertManagerWatcher(String baseUrl, GrafanaClient grafanaClient) {
        alertManagerClient = new AlertManagerClient(baseUrl);
        this.grafanaClient = grafanaClient;
    }

    public void start() {
        thread.start();
        this.stopped = false;
    }

    public void stop() throws InterruptedException {
        this.stopped = true;
        System.out.println("Alertmanager alertList: " + latestFiredAlerts.toString());
        thread.join();
    }

    @Override
    public void run() {
        while (!stopped) {
            try {
                sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            List<Alert> alerts = alertManagerClient.getAlerts();
            alerts.forEach(alert -> {
                if(addAlertToList(alert))
                    createGrafanaAnnotation(alert);
            });
        }
    }

    private void createGrafanaAnnotation(de.fhg.ipa.aas_transformer.clients.alertmanager.model.Alert alert) {
        grafanaClient.createAnnotation(
                alert.getStartsAt().toInstant(),
                alert.getStartsAt().toInstant(),
                List.of("alertmanager", "alert", alert.getAlertname()),
                "Alert: " + alert.getAlertname()
        );
    }

    private boolean addAlertToList(de.fhg.ipa.aas_transformer.clients.alertmanager.model.Alert alert) {
        String alertName = alert.getAlertname();
        OffsetDateTime currentAlertDate = latestFiredAlerts.get(alertName);
        OffsetDateTime newAlertDate = alert.getStartsAt();
        if(currentAlertDate != newAlertDate) {
            latestFiredAlerts.put(alertName, newAlertDate);
            return true;
        }
        return false;
    }
}
