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

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Destination) return false

        if (aasDestination != other.aasDestination) return false
        if (submodelDestination != other.submodelDestination) return false

        return true
    }

    override fun hashCode(): Int {
        var result = aasDestination?.hashCode() ?: 0
        result = 31 * result + (submodelDestination?.hashCode() ?: 0)
        return result
    }


}
