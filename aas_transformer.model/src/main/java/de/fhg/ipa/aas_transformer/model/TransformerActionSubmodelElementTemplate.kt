package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

class TransformerActionSubmodelElementTemplate(

    @JsonProperty("sourceSubmodelId")
    override var sourceSubmodelId: SubmodelId?,

    @JsonProperty("submodelElementTemplate")
    var submodelElementTemplate: String = "",

    ) : TransformerAction(TransformerActionType.SUBMODEL_ELEMENT_TEMPLATE, sourceSubmodelId) {

}
