package de.fhg.ipa.aas_transformer.test.system.performance.watcher;

import de.fhg.ipa.aas_transformer.clients.prometheus.PrometheusClient;
import de.fhg.ipa.aas_transformer.clients.prometheus.model.Alert;
import de.fhg.ipa.aas_transformer.clients.prometheus.model.AlertState;
import de.fhg.ipa.aas_transformer.test.utils.GrafanaClient;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Thread.sleep;

public class PrometheusAlertWatcher implements Runnable {
    Thread thread = new Thread(this);
    boolean stopped = false;
    PrometheusClient prometheusClient;
    GrafanaClient grafanaClient;
    Map<String, OffsetDateTime> latestFiredAlerts = new HashMap<>();

    public PrometheusAlertWatcher(String prometheusBaseUrl, GrafanaClient grafanaClient) {
        prometheusClient = new PrometheusClient(prometheusBaseUrl);
        this.grafanaClient = grafanaClient;
    }

    public void start() {
        thread.start();
        this.stopped = false;
    }

    public void stop() throws InterruptedException {
        this.stopped = true;
        System.out.println("Prometheus alertList: " + latestFiredAlerts.toString());
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
            List<Alert> alerts = prometheusClient.getAlerts().getData().getAlerts();
            alerts.forEach(alert -> {
                if(alert.getState().equals(AlertState.firing) && addAlertToList(alert))
                    createGrafanaAnnotation(alert);
            });
        }
    }

    private void createGrafanaAnnotation(Alert alert) {
        grafanaClient.createAnnotation(
                alert.getActiveAt().toInstant(),
                alert.getActiveAt().toInstant(),
                List.of("prometheus", "alert", alert.getAlertName()),
                "Alert: " + alert.getAlertName()
        );
    }

    private boolean addAlertToList(Alert alert) {
        String alertName = alert.getAlertName();
        OffsetDateTime currentAlertDate = latestFiredAlerts.get(alertName);
        OffsetDateTime newAlertDate = alert.getActiveAt();
        if(currentAlertDate != newAlertDate) {
            latestFiredAlerts.put(alertName, newAlertDate);
            return true;
        }
        return false;
    }
}
