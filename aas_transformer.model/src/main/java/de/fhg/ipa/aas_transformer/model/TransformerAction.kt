package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "actionType", visible = false)
@JsonSubTypes(
    JsonSubTypes.Type(value = TransformerActionCopy::class, name = "COPY"),
    JsonSubTypes.Type(value = TransformerActionSubmodelTemplate::class, name = "SUBMODEL_TEMPLATE"),
    JsonSubTypes.Type(value = TransformerActionSubmodelElementTemplate::class, name = "SUBMODEL_ELEMENT_TEMPLATE"),
    JsonSubTypes.Type(value = TransformerActionTsAvg::class, name = "TS_AVG"),
    JsonSubTypes.Type(value = TransformerActionTsMdn::class, name = "TS_MDN"),
)
abstract class TransformerAction(
    @JsonProperty("actionType")
    open var actionType: TransformerActionType? = null,

//    @JsonProperty("sourceSubmodelId")
//    open var sourceSubmodelId: SubmodelId? = null
) {
    constructor() : this(null) {}
}
