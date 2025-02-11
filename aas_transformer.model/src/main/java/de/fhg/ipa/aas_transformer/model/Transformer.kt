package de.fhg.ipa.aas_transformer.model

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.util.*

@Entity
class Transformer (

//    @javax.persistence.Id
    @Id
    @org.springframework.data.annotation.Id
    var id: UUID,

    @Column(name = "destination", columnDefinition = "LONGTEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    var destination: Destination,

    @Column(name = "transformerActions", columnDefinition = "LONGTEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    var transformerActions : MutableList<TransformerAction> = LinkedList(),

    @Column(name = "sourceSubmodelIdRules", columnDefinition = "LONGTEXT")
    @JdbcTypeCode(SqlTypes.JSON)
    var sourceSubmodelIdRules : MutableList<SourceSubmodelIdRule> = LinkedList()
) : AbstractTransformer() {
    constructor(destination: Destination, transformerActions: MutableList<TransformerAction>, sourceSubmodelIdRules: MutableList<SourceSubmodelIdRule>)
            : this(UUID.randomUUID(), destination, transformerActions, sourceSubmodelIdRules) { }
    constructor(destination: Destination)
            : this(UUID.randomUUID(), destination, LinkedList(), LinkedList()) { }
    constructor()
            :this(Destination())

    @Version
    @Column(name="OPTLOCK")
    @org.springframework.data.annotation.Version
    var version: Int = 0

    fun addTransformerAction(transformerAction : TransformerAction) {
        transformerActions.add(transformerAction)
    }

    fun addTransformerActions(transformerActions: List<TransformerAction>) {
        this.transformerActions.addAll(transformerActions)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Transformer) return false

        if (id != other.id) return false
        if (destination != other.destination) return false
        if (transformerActions != other.transformerActions) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + (destination?.hashCode() ?: 0)
        result = 31 * result + transformerActions.hashCode()
        return result
    }

}
