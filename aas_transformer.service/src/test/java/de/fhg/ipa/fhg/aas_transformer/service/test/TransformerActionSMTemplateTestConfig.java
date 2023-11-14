package de.fhg.ipa.fhg.aas_transformer.service.test;

public class TransformerActionSMTemplateTestConfig {

    public static final String SUBMODEL_TEMPLATE = """
            {
              "idShort": "operating_system",
              "identification": {
                "idType": "Custom",
                "id": "operating_system"
              },
              "dataSpecification": [],
              "embeddedDataSpecifications": [],
              "modelType": {
                "name": "Submodel"
              },
              "kind": "Instance",
              "submodelElements": {
                "distribution" : {
                  "modelType": {
                    "name": "Property"
                  },
                  "dataSpecification": [],
                  "embeddedDataSpecifications": [],
                  "kind": "Instance",
                  "value": "{{ aas:sme_value(SOURCE_AAS, 'ansible-facts', 'distribution') }}",
                  "valueType": "string",
                  "idShort": "distribution",
                  "parent": {
                    "keys": [
                      {
                        "type": "AssetAdministrationShell",
                        "local": true,
                        "value": "aas-id",
                        "idType": "Custom"
                      },
                      {
                        "type": "Submodel",
                        "local": true,
                        "value": "operating_system",
                        "idType": "Custom"
                      }
                    ]
                  }
                },
                "distribution_major_version": {
                  "modelType": {
                    "name": "Property"
                  },
                  "dataSpecification": [],
                  "embeddedDataSpecifications": [],
                  "kind": "Instance",
                  "value": "22",
                  "valueType": "string",
                  "idShort": "distribution_major_version",
                  "parent": {
                    "keys": [
                      {
                        "type": "AssetAdministrationShell",
                        "local": true,
                        "value": "aas-id",
                        "idType": "Custom"
                      },
                      {
                        "type": "Submodel",
                        "local": true,
                        "value": "operating_system",
                        "idType": "Custom"
                      }
                    ]
                  }
                }
              },
              "parent": {
                "keys": [
                  {
                    "type": "AssetAdministrationShell",
                    "local": true,
                    "value": "aas-id",
                    "idType": "Custom"
                  }
                ]
              }
            }
            """;
}
