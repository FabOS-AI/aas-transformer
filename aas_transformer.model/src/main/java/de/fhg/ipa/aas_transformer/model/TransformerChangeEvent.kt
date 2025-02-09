package de.fhg.ipa.aas_transformer.model

data class TransformerChangeEvent(
    val type: TransformerChangeEventType?,
    val transformer: Transformer?
) {
    constructor() : this(null, null)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TransformerChangeEvent) return false

        if (type != other.type) return false
        if (transformer != other.transformer) return false

        return true
    }

    override fun hashCode(): Int {
        var result = type?.hashCode() ?: 0
        result = 31 * result + (transformer?.hashCode() ?: 0)
        return result
    }
}
