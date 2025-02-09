package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

class TransformerActionCopy(
    @JsonProperty("sourceSubmodelElement")
    var sourceSubmodelElement: String
) : TransformerAction(TransformerActionType.COPY) {

    constructor(sourceSubmodelElement: String, destinationSubmodelElement: String) : this(sourceSubmodelElement) {
        this.destinationSubmodelElement = destinationSubmodelElement
    }

    constructor() : this("") {}

    @JsonProperty("destinationSubmodelElement")
    var destinationSubmodelElement: String? = null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransformerActionCopy) return false

        if (sourceSubmodelElement != other.sourceSubmodelElement) return false
        if (destinationSubmodelElement != other.destinationSubmodelElement) return false

        return true
    }

    override fun hashCode(): Int {
        var result = sourceSubmodelElement.hashCode()
        result = 31 * result + (destinationSubmodelElement?.hashCode() ?: 0)
        return result
    }


}
