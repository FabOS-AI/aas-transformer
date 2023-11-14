package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

class TransformerActionCopy(
    @JsonProperty("sourceSubmodelIdentifier")
    override var sourceSubmodelIdentifier: SubmodelIdentifier?,

    @JsonProperty("sourceSubmodelElement")
    var sourceSubmodelElement: String
) : TransformerAction(TransformerActionType.COPY, sourceSubmodelIdentifier) {

}