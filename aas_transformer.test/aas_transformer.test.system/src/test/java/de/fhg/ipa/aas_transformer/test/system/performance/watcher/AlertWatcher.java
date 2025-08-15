package de.fhg.ipa.aas_transformer.test.system.performance.watcher;

import de.fhg.ipa.aas_transformer.test.utils.GrafanaClient;

public class AlertWatcher {
    PrometheusAlertWatcher prometheusAlertWatcher;
    AlertManagerWatcher alertManagerWatcher;
    public AlertWatcher(String prometheusBaseUrl, String alertmanagerBaseUrl, GrafanaClient grafanaClient) {
        prometheusAlertWatcher = new PrometheusAlertWatcher(prometheusBaseUrl, grafanaClient);
        alertManagerWatcher = new AlertManagerWatcher(alertmanagerBaseUrl, grafanaClient);
    }

    public void start() {
        prometheusAlertWatcher.start();
        alertManagerWatcher.start();
    }

    public void stop() throws InterruptedException {
        prometheusAlertWatcher.stop();
        alertManagerWatcher.stop();
    }
}
