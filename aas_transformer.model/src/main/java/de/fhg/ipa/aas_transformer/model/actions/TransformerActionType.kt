package de.fhg.ipa.aas_transformer.model.actions

enum class TransformerActionType {
    COPY,
    SM_COPY,
    SME_RENAME,
    SUBMODEL_TEMPLATE,
    SUBMODEL_ELEMENT_TEMPLATE,
    TS_AVG,
    TS_MDN,
    TS_TAKE_EVERY,
    TS_DROP_EVERY
}
