package de.fhg.ipa.aas_transformer.model

import jakarta.persistence.*
import java.util.*

@Entity
data class TransformationDescription(
    @Id
    @org.springframework.data.annotation.Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    val id: Long? = null,
    var transformerId: UUID?,
    var sourceSubmodelId: String?,
    var targetSubmodelId: String?
) {
    constructor() : this(null, null, null, null)
}
