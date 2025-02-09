package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

class TransformerActionTsReduceDropEvery(
    @JsonProperty("n")
    override var n: Int
) : TransformerActionTsReduce( n, TransformerActionType.TS_DROP_EVERY) {
    constructor() : this(0) {}
}
