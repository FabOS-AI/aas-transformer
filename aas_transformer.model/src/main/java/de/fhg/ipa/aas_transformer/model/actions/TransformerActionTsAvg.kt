package de.fhg.ipa.aas_transformer.model.actions

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

data class TransformerActionTsAvg(
    @JsonProperty("recordNames")
    override var recordNames: MutableList<String> = LinkedList(),

    @JsonProperty("filterSize")
    override var filterSize: Int
) : TransformerActionTsFilterAbstract(
    recordNames,
    filterSize,
    TransformerActionType.TS_AVG
) {
    constructor() : this(LinkedList(), 0) {}
}
