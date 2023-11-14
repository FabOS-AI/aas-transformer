package de.fhg.ipa.aas_transformer.model

import com.vladmihalcea.hibernate.type.json.JsonStringType
import org.eclipse.basyx.submodel.metamodel.map.Submodel
import org.hibernate.annotations.Type
import org.hibernate.annotations.TypeDef
import org.hibernate.annotations.TypeDefs
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity
@TypeDefs(TypeDef(name = "json", typeClass = JsonStringType::class))
class Transformer(
    @Id
    @Type(type="uuid-char")
    @Column(name = "uuid", length = 36, unique = true, nullable = false)
    var id: UUID,

    @Column(name = "destination", columnDefinition = "LONGTEXT")
    @Type(type = "json")
    var destination: Destination,

    @Column(name = "transformerActions", columnDefinition = "LONGTEXT")
    @Type(type = "json")
    var transformerActions : LinkedList<TransformerAction> = LinkedList()
) {
    constructor(destination: Destination, transformerActions: LinkedList<TransformerAction>) : this(UUID.randomUUID(), destination, transformerActions) { }
    constructor(destination: Destination) : this(UUID.randomUUID(), destination, LinkedList()) { }

    constructor() :this(Destination())

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
