package de.fhg.ipa.aas_transformer.test.system.performance.model;

public class TransformationDurations {
    public long transformationAvg;
    public long transformationMin;
    public long transformationMax;

    public long lookupSourceAvg;
    public long lookupSourceMin;
    public long lookupSourceMax;

    public long destinationSaveAvg;
    public long destinationSaveMin;
    public long destinationSaveMax;

    public TransformationDurations(long transformationAvg, long transformationMin, long transformationMax, long lookupSourceAvg, long lookupSourceMin, long lookupSourceMax, long destinationSaveAvg, long destinationSaveMin, long destinationSaveMax) {
        this.transformationAvg = transformationAvg;
        this.transformationMin = transformationMin;
        this.transformationMax = transformationMax;
        this.lookupSourceAvg = lookupSourceAvg;
        this.lookupSourceMin = lookupSourceMin;
        this.lookupSourceMax = lookupSourceMax;
        this.destinationSaveAvg = destinationSaveAvg;
        this.destinationSaveMin = destinationSaveMin;
        this.destinationSaveMax = destinationSaveMax;
    }

    public TransformationDurations() {
    }
}
