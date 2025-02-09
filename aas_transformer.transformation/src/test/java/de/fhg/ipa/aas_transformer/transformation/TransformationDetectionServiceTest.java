package de.fhg.ipa.aas_transformer.transformation;

import de.fhg.ipa.aas_transformer.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultKey;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultReference;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.DefaultSubmodel;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestClassOrder(ClassOrderer.OrderAnnotation.class)
public class TransformationDetectionServiceTest {
    Submodel submodel1 = new DefaultSubmodel();
    String id = "id";
    String idShort = "idShort";
    String semanticId = "semanticId";

    public TransformationDetectionServiceTest() {
        submodel1.setId(id);
        submodel1.setIdShort(idShort);

        DefaultKey semanticIdKey = new DefaultKey();
        semanticIdKey.setValue(semanticId);
        DefaultReference semanticIdReference = new DefaultReference();
        semanticIdReference.setKeys(List.of(semanticIdKey));
        submodel1.setSemanticId(semanticIdReference);
    }

    private List<SourceSubmodelIdRule> getSourceSubmodelIdRules(RuleOperator ruleOperator) {
        SourceSubmodelIdRule idRule = new SourceSubmodelIdRule(ruleOperator, new SubmodelId(SubmodelIdType.ID, id));
        SourceSubmodelIdRule idShortRule = new SourceSubmodelIdRule(ruleOperator, new SubmodelId(SubmodelIdType.ID_SHORT, idShort));
        SourceSubmodelIdRule semanticIdRule = new SourceSubmodelIdRule(ruleOperator, new SubmodelId(SubmodelIdType.SEMANTIC_ID, semanticId));
        return List.of(idRule, idShortRule, semanticIdRule);
    }

    @Nested
    @Order(10)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class testOneRule {
        TransformerDTOListener transformerDTOListener = new TransformerDTOListener();
        TransformationDetectionService transformationDetectionService = new TransformationDetectionService(
                transformerDTOListener
        );

        @Test
        @Order(10)
        public void testEqualsExpectIsEqual() {
            List<SourceSubmodelIdRule> rules = getSourceSubmodelIdRules(RuleOperator.EQUALS);

            for(SourceSubmodelIdRule rule: rules) {
                transformerDTOListener.setSourceSubmodelIdRules(List.of(rule));

                assertTrue(
                        transformationDetectionService.isSubmodelSourceOfTransformerActions(submodel1)
                );
            }
        }

        @Test
        @Order(20)
        public void testNotEqualsExpectIsNotEqual() {
            List<SourceSubmodelIdRule> rules = getSourceSubmodelIdRules(RuleOperator.NOT_EQUALS);

            for(SourceSubmodelIdRule rule: rules) {
                transformerDTOListener.setSourceSubmodelIdRules(List.of(rule));

                assertFalse(
                        transformationDetectionService.isSubmodelSourceOfTransformerActions(submodel1)
                );
            }
        }
    }

    @Nested
    @Order(20)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class testThreeRules {
        TransformerDTOListener transformerDTOListener = new TransformerDTOListener();
        TransformationDetectionService transformationDetectionService = new TransformationDetectionService(
                transformerDTOListener
        );

        @Test
        @Order(10)
        public void testEqualsExpectIsEqual() {
            List<SourceSubmodelIdRule> rules = getSourceSubmodelIdRules(RuleOperator.EQUALS);
            transformerDTOListener.setSourceSubmodelIdRules(rules);

            assertTrue(
                    transformationDetectionService.isSubmodelSourceOfTransformerActions(submodel1)
            );
        }

        @Test
        @Order(20)
        public void testNotEqualsExpectIsNotEqual() {
            List<SourceSubmodelIdRule> rules = getSourceSubmodelIdRules(RuleOperator.NOT_EQUALS);
            transformerDTOListener.setSourceSubmodelIdRules(rules);

            assertFalse(
                    transformationDetectionService.isSubmodelSourceOfTransformerActions(submodel1)
            );
        }
    }

    @Nested
    @Order(30)
    @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
    public class testRegExp {
        TransformerDTOListener transformerDTOListener = new TransformerDTOListener();
        TransformationDetectionService transformationDetectionService = new TransformationDetectionService(
                transformerDTOListener
        );

        @Test
        @Order(10)
        public void testEqualsSemanticIdExpectIsEqual() {
            List<SourceSubmodelIdRule> rules = List.of(
                    new SourceSubmodelIdRule(RuleOperator.EQUALS, new SubmodelId(SubmodelIdType.SEMANTIC_ID, "^sem.*"))
            );
            transformerDTOListener.setSourceSubmodelIdRules(rules);

            assertTrue(
                    transformationDetectionService.isSubmodelSourceOfTransformerActions(submodel1)
            );
        }

        @Test
        @Order(20)
        public void testNotEqualsSemanticIdExpectIsNotEqual() {
            List<SourceSubmodelIdRule> rules = List.of(
                    new SourceSubmodelIdRule(RuleOperator.NOT_EQUALS, new SubmodelId(SubmodelIdType.SEMANTIC_ID, "SSSem.*"))
            );
            transformerDTOListener.setSourceSubmodelIdRules(rules);

            assertTrue(
                    transformationDetectionService.isSubmodelSourceOfTransformerActions(submodel1)
            );
        }
    }
}
