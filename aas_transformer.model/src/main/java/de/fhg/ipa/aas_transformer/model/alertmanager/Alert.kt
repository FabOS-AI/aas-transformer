package de.fhg.ipa.aas_transformer.model.alertmanager

import java.time.OffsetDateTime

data class Alert(
    val status: String,
    val labels: Map<String, String>,
    val annotations: Map<String, String>,
    val startsAt: OffsetDateTime,
    val endsAt: OffsetDateTime,
    val generatorURL: String,
    val fingerprint: String
)
