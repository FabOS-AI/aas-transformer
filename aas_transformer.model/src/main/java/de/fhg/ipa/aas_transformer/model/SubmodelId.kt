package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

class SubmodelId(

    @JsonProperty("type")
    val type: SubmodelIdType?,

    @JsonProperty("id")
    val id: String

) {}
