package de.fhg.ipa.aas_transformer.test.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.digitaltwin.aas4j.v3.model.*;
import org.eclipse.digitaltwin.aas4j.v3.model.impl.*;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static de.fhg.ipa.aas_transformer.aas.TimeseriesUtils.getInternalSegmentSubmodelElement;
import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.getSimpleShell;
import static de.fhg.ipa.aas_transformer.test.utils.TransformerTestObjects.getTimeseriesTransformer;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class AasTimeseriesObjects {

    public static final String TIMESERIES_SEMANTIC_ID = "https://admin-shell.io/idta/TimeSeries/1/1";

    private static ObjectMapper objectMapper = new ObjectMapper();

    public static Submodel getRandomTimeseriesSubmodel(int sensorCount, int sensorValueCount) {
        return getRandomTimeseriesSubmodel(sensorCount, sensorValueCount, "");
    }

    public static Submodel getRandomTimeseriesSubmodel(int sensorCount, int sensorValueCount, String idShort){
        DefaultSubmodel submodel = new DefaultSubmodel();
        // IDs
        submodel.setId(UUID.randomUUID().toString());
        if(idShort == null || idShort.equals("")) {
            submodel.setIdShort(UUID.randomUUID().toString().split("-")[0]);
        } else {
            submodel.setIdShort(idShort);
        }
        // SemanticId
        submodel.setSemanticId(getSemanticIdReference(TIMESERIES_SEMANTIC_ID));

        // Add Metadata and Segements to TS submodel
        submodel.setSubmodelElements(getTimeseriesSubmodelElements(sensorCount, sensorValueCount));

        return submodel;
    }

    public static List<List<Object>> getRandomTimeseriesTriples(int length) {
        String sourceIdShort = "test_timeseries";
        List<List<Object>> triples = new ArrayList<>();
        for(int i = 0; i < length; i++) {
            List<Object> triple = new ArrayList<>();
            triple.add(getSimpleShell("", ""));
            triple.add(getRandomTimeseriesSubmodel(5, 100, "test_timeseries"));
            triple.add(getTimeseriesTransformer(sourceIdShort));
            triples.add(triple);
        }
        return triples;
    }

    public static void assertInternalSegmentsEqual(
            Submodel expected,
            Submodel actual
    ) {
        SubmodelElement internalSegmentTakeEvery = getInternalSegmentSubmodelElement(expected).get();
        SubmodelElement internalSegmentAvgTakeEvery = getInternalSegmentSubmodelElement(actual).get();

        JsonNode internalSegmentTakeEveryTree = objectMapper.valueToTree(internalSegmentTakeEvery);
        JsonNode internalSegmentAvgTakeEveryTree = objectMapper.valueToTree(internalSegmentAvgTakeEvery);

        assertEquals(
                internalSegmentTakeEveryTree,
                internalSegmentAvgTakeEveryTree
        );
    }

    private static List<SubmodelElement> getTimeseriesSubmodelElements(int sensorCount, int sensorValueCount) {
        return List.of(
                getMetadataSubmodelElement(sensorCount),
                getSegementsSubmodelElement(sensorCount, sensorValueCount)
        );
    }

    private static SubmodelElement getMetadataSubmodelElement(int sensorCount) {
        // Submodel Element Collection - Metadata.Record.sensorX:
        List<SubmodelElement> sensorRecords = new ArrayList<>();
        for(int i = 0; i < sensorCount; i++) {
            sensorRecords.add(getSubmodelElement("sensor"+i, "","", DataTypeDefXsd.INTEGER));
        }

        // Submodel Element Collection - Metadata.Record:
        List<SubmodelElement> metadataRecordValues = new ArrayList<>(List.of(
                getSubmodelElement("time", "https://admin-shell.io/idta/TimeSeries/RelativePointInTime/1/1", "", DataTypeDefXsd.DATE_TIME)
        ));
        metadataRecordValues.addAll(sensorRecords);
        DefaultSubmodelElementCollection metadataRecord = new DefaultSubmodelElementCollection();
        metadataRecord.setIdShort("Record");
        metadataRecord.setSemanticId(getSemanticIdReference("https://admin-shell.io/idta/TimeSeries/Record/1/1"));
        metadataRecord.setValue(metadataRecordValues);

        // Submodel Element Collection - Metadata:
        DefaultSubmodelElementCollection metadata = new DefaultSubmodelElementCollection();
        metadata.setIdShort("Metadata");
        metadata.setSemanticId(getSemanticIdReference("https://admin-shell.io/idta/TimeSeries/Metadata/1/1"));
        List<SubmodelElement> metadataValues = new ArrayList<>(List.of(
                getSubmodelElement("Name", "https://admin-shell.io/idta/TimeSeries/Metadata/Name/1/1", "", DataTypeDefXsd.STRING),
                getSubmodelElement("Description", "https://admin-shell.io/idta/TimeSeries/Metadata/Description/1/1", "", DataTypeDefXsd.STRING),
                metadataRecord
        ));
        metadata.setValue(metadataValues);
        return metadata;
    }

    private static SubmodelElement getSegementsSubmodelElement(int sensorCount, int sensorValueCount) {
        // Submodel Element Collection - Segments:
        DefaultSubmodelElementCollection segments = new DefaultSubmodelElementCollection();
        segments.setIdShort("Segments");
        segments.setSemanticId(getSemanticIdReference("https://admin-shell.io/idta/TimeSeries/Segments/1/1"));
        segments.setValue(List.of(
                getInternalSegmentSubmodelElementCollection(sensorCount, sensorValueCount),
                getLinkedSegmentSubmodelElementCollection()
        ));
        return segments;
    }

    private static SubmodelElementCollection getLinkedSegmentSubmodelElementCollection() {
        // Submodel Element Collection - Segments.LinkedSegment:
        DefaultSubmodelElementCollection linkedSegment = new DefaultSubmodelElementCollection();
        linkedSegment.setIdShort("LinkedSegment");
        linkedSegment.setSemanticId(getSemanticIdReference("https://admin-shell.io/idta/TimeSeries/Segments/LinkedSegment/1/1"));

        return linkedSegment;
    }

    private static SubmodelElementCollection getInternalSegmentSubmodelElementCollection(int sensorCount, int sensorValueCount) {
        // Submodel Element Collection - Segments.InternalSegment.Records:
        DefaultSubmodelElementCollection internalSegmentRecords = new DefaultSubmodelElementCollection();
        internalSegmentRecords.setIdShort("Records");
        internalSegmentRecords.setSemanticId(getSemanticIdReference("https://admin-shell.io/idta/TimeSeries/Records/1/1"));
        internalSegmentRecords.setValue(getInternalSegmentRecordsOfTimeseriesSubmodel(sensorCount, sensorValueCount));

        // Submodel Element Collection - Segments.InternalSegment:
        DefaultSubmodelElementCollection internalSegment = new DefaultSubmodelElementCollection();
        internalSegment.setIdShort("InternalSegment");
        internalSegment.setSemanticId(getSemanticIdReference("https://admin-shell.io/idta/TimeSeries/Segments/InternalSegment/1/1"));
        internalSegment.setValue(List.of(internalSegmentRecords));

        return internalSegment;
    }

    private static List<SubmodelElement> getInternalSegmentRecordsOfTimeseriesSubmodel(int sensorCount, int sensorValueCount) {
        List<SubmodelElement> internalSegmentRecords = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();

        for(int i = 0; i < sensorValueCount; i++) {
            DefaultSubmodelElementCollection record = new DefaultSubmodelElementCollection();
            record.setIdShort("t_"+i);
            List<SubmodelElement> values = new ArrayList<>();
            values.add(
                    getSubmodelElement("time", "https://admin-shell.io/idta/TimeSeries/RelativePointInTime/1/1", now.plusSeconds(i).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME), DataTypeDefXsd.DATE_TIME)
            );
            values.addAll(getInternalSegmentRecord(sensorCount));

            record.setValue(values);

            internalSegmentRecords.add(record);
        }
        return internalSegmentRecords;
    }

    private static List<SubmodelElement> getInternalSegmentRecord(int sensorCount) {
        List<SubmodelElement> recordValues = new ArrayList<>();

        for(int j = 0; j < sensorCount; j++) {
            recordValues.add(getSubmodelElement(
                    "sensor"+j,
                    "",
                    String.valueOf((int) Math.floor(Math.random() * 100)),
                    DataTypeDefXsd.INTEGER
            ));
        }
        return recordValues;
    }

    private static Reference getSemanticIdReference(String semanticIdUrl) {
        DefaultReference semanticId = new DefaultReference();
        DefaultKey semanticIdKey = new DefaultKey();
        semanticIdKey.setType(KeyTypes.GLOBAL_REFERENCE);
        semanticIdKey.setValue(semanticIdUrl);
        semanticId.setType(ReferenceTypes.EXTERNAL_REFERENCE);
        semanticId.setKeys(List.of(semanticIdKey));
        return semanticId;
    }

    private static SubmodelElement getSubmodelElement(String idShort, String semanticId, String value, DataTypeDefXsd type) {
        DefaultProperty property = new DefaultProperty();
        property.setIdShort(idShort);
        property.setValue(value);
        property.setSemanticId(getSemanticIdReference(semanticId));
        property.setValueType(type);
        return property;
    }
}
