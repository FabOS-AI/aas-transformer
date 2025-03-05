package de.fhg.ipa.aas_transformer.clients.prometheus.model

data class AlertResponse(
    val status: String = "",
    val data: AlertResponseData? = null
)
