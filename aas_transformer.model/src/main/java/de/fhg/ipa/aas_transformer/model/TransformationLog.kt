package de.fhg.ipa.aas_transformer.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import java.time.LocalDateTime
import java.util.*

@Entity
class TransformationLog(

    @Column
    val destinationAasId: String,

    @Column
    val destinationSubmodelId: String,

    @Column
    val sourceSubmodelId: String,

    @Column
    val executorId: UUID? = null,

    @Column
    val transformationDurationInMs: Long? = null,

    @Column
    val sourceLookupInMs: Long? = null,

    @Column
    val destinationSaveInMs: Long? = null
) {
    constructor()
            : this("","","")
    @Id
    @org.springframework.data.annotation.Id
    var id: Long? = null

    @Column
    var date: LocalDateTime = LocalDateTime.now()
    override fun toString(): String {
        return "TransformationLog(destinationAasId='$destinationAasId', destinationSubmodelId='$destinationSubmodelId', sourceSubmodelId='$sourceSubmodelId', date=$date)"
    }
}
