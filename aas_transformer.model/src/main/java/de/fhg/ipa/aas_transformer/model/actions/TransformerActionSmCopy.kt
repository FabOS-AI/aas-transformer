package de.fhg.ipa.aas_transformer.model.actions

import com.fasterxml.jackson.annotation.JsonProperty

class TransformerActionSmCopy(
    @JsonProperty("submodelId")
    var submodelId: String
) : TransformerAction(TransformerActionType.SM_COPY) {
    constructor() : this("") {}
}
