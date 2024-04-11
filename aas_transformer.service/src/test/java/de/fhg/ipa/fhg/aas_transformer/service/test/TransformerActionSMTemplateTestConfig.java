package de.fhg.ipa.fhg.aas_transformer.service.test;

public class TransformerActionSMTemplateTestConfig {

    public static final String SUBMODEL_TEMPLATE = """
            {
              "modelType": "Submodel",
              "submodelElements": [
                {
                  "idShort": "TemplatedSimpleProperty",
                  "modelType": "Property",
                  "semanticId": {
                    "type": "ExternalReference",
                    "keys": [
                      {
                        "type": "GlobalReference",
                        "value": "http://acplt.org/Properties/SimpleProperty"
                      }
                    ]
                  },
                  "value": "{{ submodel:sme_value(SOURCE_SUBMODEL, 'SimpleProperty2') }}",
                  "valueType": "xs:string"
                },
                {
                  "idShort": "SimpleProperty2",
                  "modelType": "Property",
                  "semanticId": {
                    "type": "ExternalReference",
                    "keys": [
                      {
                        "type": "GlobalReference",
                        "value": "http://acplt.org/Properties/SimpleProperty2"
                      }
                    ]
                  },
                  "value": "SimpleValue2",
                  "valueType": "xs:string"
                }
              ]
            }
            """;
}
