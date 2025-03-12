package de.fhg.ipa.aas_transformer.model

import java.util.*

data class TransformerDTOListener(
    var id: UUID?,
    var sourceSubmodelIdRules : MutableList<SourceSubmodelIdRule> = LinkedList(),
    var transformOnRequest: Boolean = false
) : AbstractTransformer() {
    constructor() : this(null)
}
