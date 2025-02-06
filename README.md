# AAS Transformer

The AAS transformer is capable of creating new submodels (target) based on the information from already available submodels (source) in a company’s network. The AAS transformer supports companies: 
- to develop new submodel templates, 
- to derive submodels from older submodels in case the submodule template gets revised, 
- to run intra-company and standardized submodels in parallel that may have an intersection, and 
- to enable generic products to provide multiple domain-specific submodules depending on their current operation area 

The concept of the AAS transformer is described in detail in the paper [Concept of Event-based AAS Transformation Engine](https://www.sciencedirect.com/science/article/pii/S221282712401391X).

## Setup
To setup the AAS transformer including an AAS infracustred with [Eclipse BaSyx components](https://github.com/eclipse-basyx/basyx-java-server-sdk) use the `docker-compose.yml` in the direcotry `docker-compose`
```bash
cd docker-compose
docker-compose.yml
```
