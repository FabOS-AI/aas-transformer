package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

class SubmodelIdentifier(
    @JsonProperty("identifierType")
    val identifierType: SubmodelIdentifierType?,
    @JsonProperty("identifierValue")
    val identifierValue: String
) {}