package de.fhg.ipa.fhg.aas_transformer.service.test;

public class TransformerActionSMETemplateTestConfig {

    public static final String SUBMODEL_ELEMENT_TEMPLATE = """
            {
              "modelType": {
                "name": "Property"
              },
              "dataSpecification": [],
              "embeddedDataSpecifications": [],
              "kind": "Instance",
              "value": "{{ aas:sme_value(SOURCE_AAS, 'ansible-facts', 'distribution') }}",
              "valueType": "string",
              "idShort": "distribution_templated"
            }
            """;
}
