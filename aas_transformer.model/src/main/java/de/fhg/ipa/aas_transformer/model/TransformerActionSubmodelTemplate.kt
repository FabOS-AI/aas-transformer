package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

class TransformerActionSubmodelTemplate(
    @JsonProperty("submodelTemplate")
    var submodelTemplate: String = "",

    ) : TransformerAction(TransformerActionType.SUBMODEL_TEMPLATE) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransformerActionSubmodelTemplate) return false

        if (submodelTemplate != other.submodelTemplate) return false

        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}
