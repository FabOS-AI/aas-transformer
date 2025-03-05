package de.fhg.ipa.aas_transformer.clients.prometheus.model

import java.time.OffsetDateTime

data class Alert(
    val activeAt: OffsetDateTime? = null,
    val annotations: Map<String, String> = HashMap(),
    val labels: Map<String, String> = HashMap(),
    val state: AlertState? = null,
    val value: String = ""
) {
    fun getAlertName(): String? {
        return labels["alertname"]
    }
}
