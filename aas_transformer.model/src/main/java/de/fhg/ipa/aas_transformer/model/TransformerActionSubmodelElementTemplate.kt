package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

class TransformerActionSubmodelElementTemplate(
    @JsonProperty("destinationSubmodelElementId")
    var destinationSubmodelElementId: String = "",

    @JsonProperty("submodelElementTemplate")
    var submodelElementTemplate: String = "",

    ) : TransformerAction(TransformerActionType.SUBMODEL_ELEMENT_TEMPLATE) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransformerActionSubmodelElementTemplate) return false

        if (destinationSubmodelElementId != other.destinationSubmodelElementId) return false
        if (submodelElementTemplate != other.submodelElementTemplate) return false

        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }


}
