package de.fhg.ipa.aas_transformer.model.alertmanager

data class AlertMessage(
    val receiver: String,
    val status: String,
    val alerts: List<Alert>,
    val groupLabels: Map<String, String>,
    val commonLabels: Map<String, String>,
    val commonAnnotations: Map<String, String>,
    val externalURL: String,
    val version: String,
    val groupKey: String,
    val truncatedAlerts: Int
)
