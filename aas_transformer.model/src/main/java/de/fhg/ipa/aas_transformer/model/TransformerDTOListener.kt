package de.fhg.ipa.aas_transformer.model

import java.util.*

data class TransformerDTOListener(
    var id: UUID?,
    var destination: Destination? = null,
    var sourceSubmodelIdRules : MutableList<SourceSubmodelIdRule> = LinkedList(),
    var transformOnRequest: Boolean = false
) : AbstractTransformer() {
    constructor() : this(null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransformerDTOListener) return false

        if (transformOnRequest != other.transformOnRequest) return false
        if (id != other.id) return false
        if (destination != other.destination) return false
        if (sourceSubmodelIdRules != other.sourceSubmodelIdRules) return false

        return true
    }

    override fun hashCode(): Int {
        var result = transformOnRequest.hashCode()
        result = 31 * result + (id?.hashCode() ?: 0)
        result = 31 * result + (destination?.hashCode() ?: 0)
        result = 31 * result + sourceSubmodelIdRules.hashCode()
        return result
    }


}
