package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

class TransformerActionCopy(
    @JsonProperty("sourceSubmodelId")
    override var sourceSubmodelId: SubmodelId?,

    @JsonProperty("sourceSubmodelElement")
    var sourceSubmodelElement: String
) : TransformerAction(TransformerActionType.COPY, sourceSubmodelId) {

    @JsonProperty("destinationSubmodelElement")
    var destinationSubmodelElement: String? = null

}
