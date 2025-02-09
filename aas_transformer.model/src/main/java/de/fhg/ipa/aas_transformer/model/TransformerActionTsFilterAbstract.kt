package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty
import java.util.*

abstract class TransformerActionTsFilterAbstract(
    @JsonProperty("recordNames")
    open var recordNames: MutableList<String> = LinkedList(),

    @JsonProperty("filterSize")
    open var filterSize: Int,

    @JsonProperty("transformerActionType")
    var transformerActionType: TransformerActionType
) : TransformerAction(transformerActionType)
