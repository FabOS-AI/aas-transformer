package de.fhg.ipa.aas_transformer.model

class Destination () {

    var aasDestination: DestinationAAS? = null

    var smDestination: DestinationSM? = null

    constructor (smDestination: DestinationSM) : this() {
        this.smDestination = smDestination
    }

    constructor (aasDestination: DestinationAAS, smDestination: DestinationSM) : this (smDestination) {
        this.aasDestination = aasDestination
    }

}
