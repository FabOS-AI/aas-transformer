package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

class SubmodelId(

    @JsonProperty("type")
    val type: SubmodelIdType?,

    @JsonProperty("id")
    val id: String

) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SubmodelId) return false

        if (type != other.type) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type?.hashCode() ?: 0
        result = 31 * result + id.hashCode()
        return result
    }
}
