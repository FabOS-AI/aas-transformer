package de.fhg.ipa.aas_transformer.model.alertmanager

data class AlertStatus(
    val inhibitedBy: List<String> = emptyList(),
    val mutedBy: List<String> = emptyList(),
    val silencedBy: List<String> = emptyList(),
    val state: String = ""
)
