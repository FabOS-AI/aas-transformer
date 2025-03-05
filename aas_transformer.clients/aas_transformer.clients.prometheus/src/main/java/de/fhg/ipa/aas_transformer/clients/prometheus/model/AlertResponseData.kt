package de.fhg.ipa.aas_transformer.clients.prometheus.model

import java.util.*

data class AlertResponseData(
    val alerts: List<Alert> = LinkedList()
)
