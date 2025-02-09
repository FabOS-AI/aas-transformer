package de.fhg.ipa.aas_transformer.model

data class TransformerChangeEventDTOListener(
    val type: TransformerChangeEventType?,
    val transformer: TransformerDTOListener?
) {
    constructor() : this(null, null)
}
