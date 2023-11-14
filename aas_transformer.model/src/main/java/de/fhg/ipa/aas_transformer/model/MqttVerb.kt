package de.fhg.ipa.aas_transformer.model

enum class MqttVerb(val mqttVerb: String) {
    CREATED("created"),
    UPDATED("updated"),
    DELETED("deleted")
}