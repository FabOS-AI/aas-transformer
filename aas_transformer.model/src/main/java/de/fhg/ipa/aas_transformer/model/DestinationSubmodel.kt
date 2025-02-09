package de.fhg.ipa.aas_transformer.model

class DestinationSubmodel (
    val idShort: String,
    val id: String)
{
    constructor(): this("", "") {}

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DestinationSubmodel) return false

        if (idShort != other.idShort) return false
        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        var result = idShort.hashCode()
        result = 31 * result + id.hashCode()
        return result
    }
}
