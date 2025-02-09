package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

class TransformerActionTsReduceTakeEvery(
    @JsonProperty("n")
    override var n: Int
) : TransformerActionTsReduce(n, TransformerActionType.TS_TAKE_EVERY) {

    constructor() : this(0) {}
}
