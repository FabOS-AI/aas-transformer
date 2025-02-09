package de.fhg.ipa.aas_transformer.model

import org.eclipse.digitaltwin.aas4j.v3.model.Submodel
import java.util.UUID

data class TransformationJob(
    var transformationJobAction: TransformationJobAction,
    var transformerId: UUID?,
    var submodelId: String?,
    var submodel: Submodel?
) {
    override fun toString(): String {
        return "TransformationJob(transformationJobAction=$transformationJobAction, transformerId=$transformerId, submodelId=$submodelId, submodel=$submodel)"
    }
}
