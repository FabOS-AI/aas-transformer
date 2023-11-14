package de.fhg.ipa.aas_transformer.model

import org.eclipse.basyx.submodel.metamodel.map.identifier.Identifier

class DestinationAAS () {

    var idShort: String? = null

    var identifier: Identifier? = null

    var endpoint: String = ""

    constructor(idShort: String) : this() {
        this.idShort = idShort
    }

    constructor(identifier: Identifier) : this() {
        this.identifier = identifier
    }

    constructor(idShort: String, identifier: Identifier) : this() {
        this.idShort = idShort
        this.identifier = identifier
    }
}
