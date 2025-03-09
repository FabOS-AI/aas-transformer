package de.fhg.ipa.aas_transformer.model.alertmanager

data class AlertMessage(
    val receiver: String = "",
    val status: String = "",
    val alerts: List<Alert> = emptyList(),
    val groupLabels: Map<String, String> = emptyMap(),
    val commonLabels: Map<String, String> = emptyMap(),
    val commonAnnotations: Map<String, String> = emptyMap(),
    val externalURL: String = "",
    val version: String = "",
    val groupKey: String = "",
    val truncatedAlerts: Int = 0
)
