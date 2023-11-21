package de.fhg.ipa.fhg.aas_transformer.service.test;

import de.fhg.ipa.aas_transformer.service.Application;
import de.fhg.ipa.aas_transformer.service.actions.TransformerActionServiceFactory;
import de.fhg.ipa.aas_transformer.service.aas.AASManager;
import de.fhg.ipa.aas_transformer.service.aas.AASRegistry;
import de.fhg.ipa.aas_transformer.service.mqtt.AASMqttListener;
import de.fhg.ipa.aas_transformer.service.mqtt.SubmodelElementMqttListener;
import de.fhg.ipa.aas_transformer.service.mqtt.SubmodelMqttListener;
import org.eclipse.basyx.vab.exception.provider.ResourceNotFoundException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(AasITExtension.class)
@SpringBootTest(classes = { Application.class, TransformerActionServiceFactory.class })
@ActiveProfiles("test")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class BrokerListenerTest {

    @SpyBean
    private AASMqttListener aasMqttListener;

    @SpyBean
    private SubmodelMqttListener submodelMqttListener;

    @SpyBean
    private SubmodelElementMqttListener submodelElementMqttListener;

    @Autowired
    public AASManager aasManager;

    @Autowired
    private AASRegistry aasRegistry;

    @Test
    @Order(10)
    public void testContextLoaded() {
        assertThat(aasMqttListener).isNotNull();
        assertThat(submodelMqttListener).isNotNull();
    }

    @Nested
    @Order(20)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class testAasCrudOperations {
        @Test
        @Order(10)
        public void testCreateAas() {
            aasManager.createAAS(GenericTestConfig.AAS);

            Mockito.verifyNoInteractions(submodelMqttListener);
        }

        @Test
        @Order(20)
        public void testCreateSubmodel() throws Exception {
            aasManager.createSubmodel(
                    GenericTestConfig.AAS.getIdentification(),
                    GenericTestConfig.getAnsibleFactsSubmodel()
            );

            ArgumentCaptor<String> argumentTopic = ArgumentCaptor.forClass(String.class);
            Mockito.verify(submodelMqttListener, Mockito.atLeast(3)).messageArrived(argumentTopic.capture(), Mockito.any());
            assertThat(argumentTopic.getAllValues())
                    .contains("aas-registry/aas-registry/shells/aas-id/submodels/created")
                    .contains("aas-repository/aas-server/shells/aas-id/submodels/created")
                    .contains("aas-registry/aas-registry/shells/aas-id/submodels/updated");
        }

        @Test
        @Order(30)
        public void testUpdateSubmodelElement() throws Exception {
            String keyOfSubmodelElement = "distribution";
            String newValueOfSubmodelElement = "Windows";

            var sourceSubmodel = aasManager.retrieveSubmodel(
                    GenericTestConfig.AAS.getIdentification(),
                    GenericTestConfig.getAnsibleFactsIdentifier()
            );
            //Change value of SubmodelElement with key 'distribution'
            sourceSubmodel.getSubmodelElement(keyOfSubmodelElement).setValue(newValueOfSubmodelElement);

            ArgumentCaptor<String> argumentTopic = ArgumentCaptor.forClass(String.class);
            Mockito.verify(submodelElementMqttListener).messageArrived(argumentTopic.capture(), Mockito.any());
            assertThat(argumentTopic.getValue())
                    .isEqualTo("aas-repository/aas-server/shells/aas-id/submodels/ansible-facts/submodelElements/distribution/value");
        }

        @Test
        @Order(40)
        public void testDeleteSubmodel() throws Exception {
            try {
                aasManager.deleteSubmodel(
                        GenericTestConfig.AAS.getIdentification(),
                        GenericTestConfig.getAnsibleFactsIdentifier()
                );
            } catch(ResourceNotFoundException e) {}

            Thread.sleep(100);

            ArgumentCaptor<String> argumentTopic = ArgumentCaptor.forClass(String.class);
            Mockito.verify(submodelMqttListener, Mockito.atLeast(1)).messageArrived(argumentTopic.capture(), Mockito.any());
            assertThat(argumentTopic.getAllValues())
                    .contains("aas-registry/aas-registry/shells/aas-id/submodels/deleted")
                    .contains("aas-repository/aas-server/shells/aas-id/submodels/deleted");
        }
    }
}
