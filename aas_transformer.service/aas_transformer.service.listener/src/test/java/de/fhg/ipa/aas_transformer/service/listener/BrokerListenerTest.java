package de.fhg.ipa.aas_transformer.service.listener;

import de.fhg.ipa.aas_transformer.service.listener.mqtt.BrokerListener;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.model.SubmodelChangeEventType;
import de.fhg.ipa.aas_transformer.service.listener.events.SubmodelMessageEvent;
import de.fhg.ipa.aas_transformer.service.listener.mqtt.SubmodelElementMqttListener;
import de.fhg.ipa.aas_transformer.service.listener.mqtt.SubmodelMqttListener;
import de.fhg.ipa.aas_transformer.test.utils.extentions.AasITExtension;
import de.fhg.ipa.aas_transformer.test.utils.GenericTestConfig;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static java.lang.Thread.sleep;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(AasITExtension.class)
@ContextConfiguration(classes = {
        BrokerListener.class,
        SubmodelMqttListener.class,
        SubmodelElementMqttListener.class,
        SubmodelRegistry.class,
        SubmodelRepository.class
})
@ActiveProfiles("test")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public class BrokerListenerTest {
    @SpyBean
    private SubmodelMqttListener submodelMqttListener;
    @SpyBean
    private BrokerListener brokerListener;

    @Autowired
    public SubmodelRepository submodelRepository;

    @Test
    @Order(10)
    public void testContextLoaded() {
        assertThat(submodelMqttListener).isNotNull();
        assertThat(brokerListener).isNotNull();
    }

    @Nested
    @Order(20)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class testAasCrudOperations {

        @Test
        @Order(10)
        public void testCreateSubmodel() throws Exception {
            submodelRepository.createOrUpdateSubmodel(GenericTestConfig.getSimpleSubmodel());

            ArgumentCaptor<String> argumentTopic = ArgumentCaptor.forClass(String.class);
            int tryCount = 0;
            int tryLimit = 10;
            while(true) {
                tryCount++;
                if(tryCount > tryLimit)
                    throw new AssertionError("Method messageArrived() was not called within the expected retries.");
                try {
                    Mockito
                            .verify(submodelMqttListener, Mockito.timeout(500).atLeast(1))
                            .messageArrived(argumentTopic.capture(), Mockito.any());
                    break;
                } catch(org.mockito.exceptions.verification.VerificationInOrderFailure |
                        org.mockito.exceptions.verification.NoInteractionsWanted |
                        org.mockito.exceptions.verification.TooManyActualInvocations |
                        org.mockito.exceptions.verification.WantedButNotInvoked e) {
                    System.out.println("Try #"+tryCount+" failed.");
                    sleep(500);
                }
            }

//            Mockito.verify(brokerListener, Mockito.atLeast(1)).messageArrived(argumentTopic.capture(), Mockito.any());
            assertThat(argumentTopic.getAllValues())
                    .contains("sm-repository/sm-repo/submodels/created");
            sleep(100);
            assertThat(submodelMqttListener.getMessageEventCache()).hasSize(1);
            SubmodelMessageEvent submodelMessageEvent = (SubmodelMessageEvent)submodelMqttListener.getMessageEventCache().poll();
            assertThat(submodelMessageEvent.getChangeEventType()).isEqualTo(SubmodelChangeEventType.CREATED);
            assertThat(submodelMessageEvent.getSubmodel().getId()).isEqualTo(GenericTestConfig.getSimpleSubmodel().getId());
        }

        @Test
        @Order(30)
        public void testUpdateSubmodelElement() throws Exception {
            String keyOfSubmodelElement = "SimpleProperty";
            String newValueOfSubmodelElement = "NewValue";

            var updatedSubmodel = GenericTestConfig.getSimpleSubmodel();
            var submodelElements = updatedSubmodel.getSubmodelElements();
            var submodelElementToUpdateOptional = submodelElements.stream()
                    .filter(se -> se.getIdShort().equals(keyOfSubmodelElement)).findAny();
            var updatedSubmodelElement = ((Property)submodelElementToUpdateOptional.get());
            updatedSubmodelElement.setValue(newValueOfSubmodelElement);
            submodelElements.remove(submodelElementToUpdateOptional.get());
            submodelElements.add(updatedSubmodelElement);
            updatedSubmodel.setSubmodelElements(submodelElements);

            submodelRepository.createOrUpdateSubmodel(updatedSubmodel);

            ArgumentCaptor<String> argumentTopic = ArgumentCaptor.forClass(String.class);
            Mockito.verify(submodelMqttListener).messageArrived(argumentTopic.capture(), Mockito.any());
            assertThat(argumentTopic.getAllValues())
                    .contains("sm-repository/sm-repo/submodels/updated");
            sleep(100);
            assertThat(submodelMqttListener.getMessageEventCache()).hasSize(1);
            SubmodelMessageEvent submodelMessageEvent = (SubmodelMessageEvent)submodelMqttListener.getMessageEventCache().poll();
            assertThat(submodelMessageEvent.getChangeEventType()).isEqualTo(SubmodelChangeEventType.UPDATED);
            assertThat(submodelMessageEvent.getSubmodel().getId()).isEqualTo(GenericTestConfig.getSimpleSubmodel().getId());
            updatedSubmodelElement = (Property)submodelMessageEvent.getSubmodel().getSubmodelElements().stream()
                    .filter(se -> se.getIdShort().equals(keyOfSubmodelElement)).findAny().get();
            assertThat(updatedSubmodelElement.getValue()).isEqualTo(newValueOfSubmodelElement);
        }

        @Test
        @Order(40)
        public void testDeleteSubmodel() throws Exception {
            submodelRepository.deleteSubmodel(GenericTestConfig.getSimpleSubmodel().getId());

            ArgumentCaptor<String> argumentTopic = ArgumentCaptor.forClass(String.class);
            Mockito.verify(submodelMqttListener, Mockito.atLeast(1)).messageArrived(argumentTopic.capture(), Mockito.any());
            assertThat(argumentTopic.getAllValues())
                    .contains("sm-repository/sm-repo/submodels/deleted");
            sleep(100);
            assertThat(submodelMqttListener.getMessageEventCache()).hasSize(1);
            SubmodelMessageEvent submodelMessageEvent = (SubmodelMessageEvent)submodelMqttListener.getMessageEventCache().poll();
            assertThat(submodelMessageEvent.getChangeEventType()).isEqualTo(SubmodelChangeEventType.DELETED);
            assertThat(submodelMessageEvent.getSubmodel().getId()).isEqualTo(GenericTestConfig.getSimpleSubmodel().getId());
        }
    }
}
