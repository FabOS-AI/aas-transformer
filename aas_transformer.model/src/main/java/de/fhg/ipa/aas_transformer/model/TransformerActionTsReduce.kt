package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

abstract class TransformerActionTsReduce(
    @JsonProperty("n")
    open var n: Int,

    @JsonProperty("transformerActionType")
    val transformerActionType: TransformerActionType?
) : TransformerAction(transformerActionType) {
    constructor() : this(0, null) {}
}
