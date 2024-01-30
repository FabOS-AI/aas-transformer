package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonTypeName

class TransformerActionCopy(
    @JsonProperty("sourceSubmodelIdentifier")
    override var sourceSubmodelIdentifier: SubmodelIdentifier?,

    @JsonProperty("sourceSubmodelElement")
    var sourceSubmodelElement: String
) : TransformerAction(TransformerActionType.COPY, sourceSubmodelIdentifier) {

    @JsonProperty("destinationSubmodelElement")
    var destinationSubmodelElement: String? = null

}
