package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

class TransformerActionSubmodelTemplate(

    @JsonProperty("sourceSubmodelIdentifier")
    override var sourceSubmodelId: SubmodelId?,

    @JsonProperty("submodelTemplate")
    var submodelTemplate: String = "",

    ) : TransformerAction(TransformerActionType.SUBMODEL_TEMPLATE, sourceSubmodelId) {

}
