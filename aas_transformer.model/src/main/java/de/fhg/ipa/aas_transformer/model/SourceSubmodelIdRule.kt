package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

data class SourceSubmodelIdRule(
    @JsonProperty("ruleOperator")
    var ruleOperator: RuleOperator? = null,
    @JsonProperty("sourceSubmodelId")
    var sourceSubmodelId: SubmodelId? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SourceSubmodelIdRule) return false

        if (ruleOperator != other.ruleOperator) return false
        if (sourceSubmodelId != other.sourceSubmodelId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = ruleOperator?.hashCode() ?: 0
        result = 31 * result + (sourceSubmodelId?.hashCode() ?: 0)
        return result
    }
}
