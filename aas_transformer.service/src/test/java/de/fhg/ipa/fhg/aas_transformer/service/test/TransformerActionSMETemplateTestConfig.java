package de.fhg.ipa.fhg.aas_transformer.service.test;

public class TransformerActionSMETemplateTestConfig {

    public static final String SUBMODEL_ELEMENT_TEMPLATE = """
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
            }
            """;
}
