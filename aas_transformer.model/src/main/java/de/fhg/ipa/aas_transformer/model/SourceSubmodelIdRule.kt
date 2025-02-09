package de.fhg.ipa.aas_transformer.model

import com.fasterxml.jackson.annotation.JsonProperty

data class SourceSubmodelIdRule(
    @JsonProperty("ruleOperator")
    var ruleOperator: RuleOperator? = null,
    @JsonProperty("sourceSubmodelId")
    var sourceSubmodelId: SubmodelId? = null,
)
