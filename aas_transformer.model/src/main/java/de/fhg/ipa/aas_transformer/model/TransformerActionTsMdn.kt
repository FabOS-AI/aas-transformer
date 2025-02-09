package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class TransformerActionTsMdn(
    @JsonProperty("recordNames")
    override var recordNames: MutableList<String> = LinkedList(),

    @JsonProperty("filterSize")
    override var filterSize: Int
) : TransformerActionTsFilterAbstract(
    recordNames,
    filterSize,
    TransformerActionType.TS_MDN
) {
    constructor() : this( LinkedList(), 0) {}
}
