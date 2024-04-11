package de.fhg.ipa.fhg.aas_transformer.service.test;

import de.fhg.ipa.aas_transformer.service.BrokerListener;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.service.aas.SubmodelRepository;
import de.fhg.ipa.aas_transformer.service.events.ChangeEventType;
import de.fhg.ipa.aas_transformer.service.events.SubmodelMessageEvent;
import de.fhg.ipa.aas_transformer.service.mqtt.SubmodelElementMqttListener;
import de.fhg.ipa.aas_transformer.service.mqtt.SubmodelMqttListener;
import org.eclipse.digitaltwin.aas4j.v3.model.Property;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(AasITExtension.class)
@ContextConfiguration(classes = {
        BrokerListener.class,
        SubmodelMqttListener.class, SubmodelElementMqttListener.class,
        SubmodelRegistry.class, SubmodelRepository.class })
@ActiveProfiles("test")
@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class BrokerListenerTest {

    @SpyBean
    private SubmodelMqttListener submodelMqttListener;

    @Autowired
    public SubmodelRepository submodelRepository;

    @Test
    @Order(10)
    public void testContextLoaded() {
        assertThat(submodelMqttListener).isNotNull();
    }

    @Nested
    @Order(20)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class testAasCrudOperations {

        @Test
        @Order(10)
        public void testCreateSubmodel() throws Exception {
            Thread.sleep(500);
            submodelRepository.createOrUpdateSubmodel(GenericTestConfig.getSimpleSubmodel());
            Thread.sleep(100);

            ArgumentCaptor<String> argumentTopic = ArgumentCaptor.forClass(String.class);
            Mockito.verify(submodelMqttListener, Mockito.atLeast(1)).messageArrived(argumentTopic.capture(), Mockito.any());
            assertThat(argumentTopic.getAllValues())
                    .contains("sm-repository/sm-repo/submodels/created");
            Thread.sleep(100);
            assertThat(submodelMqttListener.getMessageEventCache()).hasSize(1);
            SubmodelMessageEvent submodelMessageEvent = (SubmodelMessageEvent)submodelMqttListener.getMessageEventCache().poll();
            assertThat(submodelMessageEvent.getChangeEventType()).isEqualTo(ChangeEventType.CREATED);
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
            Thread.sleep(100);
            assertThat(submodelMqttListener.getMessageEventCache()).hasSize(1);
            SubmodelMessageEvent submodelMessageEvent = (SubmodelMessageEvent)submodelMqttListener.getMessageEventCache().poll();
            assertThat(submodelMessageEvent.getChangeEventType()).isEqualTo(ChangeEventType.UPDATED);
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
            Thread.sleep(100);
            assertThat(submodelMqttListener.getMessageEventCache()).hasSize(1);
            SubmodelMessageEvent submodelMessageEvent = (SubmodelMessageEvent)submodelMqttListener.getMessageEventCache().poll();
            assertThat(submodelMessageEvent.getChangeEventType()).isEqualTo(ChangeEventType.DELETED);
            assertThat(submodelMessageEvent.getSubmodel().getId()).isEqualTo(GenericTestConfig.getSimpleSubmodel().getId());
        }
    }
}
