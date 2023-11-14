package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import org.eclipse.basyx.submodel.metamodel.api.reference.IKey
import org.eclipse.basyx.submodel.metamodel.map.Submodel

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "actionType")
@JsonSubTypes(
    JsonSubTypes.Type(value = TransformerActionCopy::class,      name = "COPY")
)

abstract class TransformerAction(
    @JsonProperty("actionType")
    val actionType: TransformerActionType,
    @JsonProperty("sourceSubmodelIdentifier")
    open var sourceSubmodelIdentifier: SubmodelIdentifier? = null
) {

}
