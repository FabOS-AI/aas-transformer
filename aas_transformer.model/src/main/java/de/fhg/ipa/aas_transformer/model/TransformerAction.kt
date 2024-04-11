package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "actionType", visible = true)
@JsonSubTypes(
    JsonSubTypes.Type(value = TransformerActionCopy::class, name = "COPY"),
    JsonSubTypes.Type(value = TransformerActionSubmodelTemplate::class, name = "SUBMODEL_TEMPLATE"),
    JsonSubTypes.Type(value = TransformerActionSubmodelElementTemplate::class, name = "SUBMODEL_ELEMENT_TEMPLATE")
)
abstract class TransformerAction(

    @JsonProperty("actionType")
    var actionType: TransformerActionType,

    @JsonProperty("sourceSubmodelId")
    open var sourceSubmodelId: SubmodelId? = null

) {
}
