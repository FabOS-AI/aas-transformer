package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

class TransformerActionSubmodelElementTemplate(

    @JsonProperty("sourceSubmodelIdentifier")
    override var sourceSubmodelIdentifier: SubmodelIdentifier?,

    @JsonProperty("submodelElementTemplate")
    var submodelElementTemplate: String = "",

) : TransformerAction(TransformerActionType.SUBMODEL_ELEMENT_TEMPLATE, sourceSubmodelIdentifier) {

}
