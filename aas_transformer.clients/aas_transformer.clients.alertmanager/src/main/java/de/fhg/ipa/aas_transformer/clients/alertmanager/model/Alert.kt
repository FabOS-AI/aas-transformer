package de.fhg.ipa.aas_transformer.clients.alertmanager.model

import java.time.OffsetDateTime

data class Alert(
    val status: AlertStatus = AlertStatus(),
    val labels: Map<String, String> = emptyMap(),
    val annotations: Map<String, String> = emptyMap(),
    val startsAt: OffsetDateTime? = null,
    val updatedAt: OffsetDateTime? = null,
    val endsAt: OffsetDateTime? = null,
    val receivers: List<Map<String,String>> = emptyList(),
    val generatorURL: String = "",
    val fingerprint: String = ""
) {
    fun getAlertname(): String {
        return labels["alertname"] ?: ""
    }
}
