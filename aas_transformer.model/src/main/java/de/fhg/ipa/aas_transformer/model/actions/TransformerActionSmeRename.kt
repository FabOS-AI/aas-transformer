package de.fhg.ipa.aas_transformer.model.actions

import com.fasterxml.jackson.annotation.JsonProperty
import de.fhg.ipa.aas_transformer.model.SubmodelElementProperty

class TransformerActionSmeRename (
    // Submodel Element path to SE in intermediate result of transformation
    @param:JsonProperty("smePath")
    var smePath: String = "",

    // Property that shall receive the new value
    @param:JsonProperty("submodelElementProperty")
    var submodelElementProperty: SubmodelElementProperty,

    // new value of SME's property
    @param:JsonProperty("newValue")
    var newValue: String = ""
) : TransformerAction(TransformerActionType.SME_RENAME) {}