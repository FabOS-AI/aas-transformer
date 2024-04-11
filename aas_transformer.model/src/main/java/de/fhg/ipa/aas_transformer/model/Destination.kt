package de.fhg.ipa.aas_transformer.model

class Destination () {

    var aasDestination: DestinationAAS? = null

    var submodelDestination: DestinationSubmodel? = null

    constructor (submodelDestination: DestinationSubmodel) : this() {
        this.submodelDestination = submodelDestination
    }

    constructor (aasDestination: DestinationAAS, submodelDestination: DestinationSubmodel) : this (submodelDestination) {
        this.aasDestination = aasDestination
    }

}
