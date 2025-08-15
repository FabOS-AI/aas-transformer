package de.fhg.ipa.aas_transformer.test.utils.creator;


import de.fhg.ipa.aas_transformer.aas.AasRegistry;
import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRegistry;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import org.eclipse.digitaltwin.aas4j.v3.model.Submodel;

import java.util.LinkedList;
import java.util.List;

import static de.fhg.ipa.aas_transformer.test.utils.AasTestObjects.registerAasObjectsFromTriples;
import static de.fhg.ipa.aas_transformer.test.utils.AasTimeseriesObjects.getRandomTimeseriesTriples;
import static java.lang.Thread.sleep;

abstract public class TimeSeriesSubmodelCreator implements Runnable {
    // AAS Clients:
    private AasRegistry aasRegistry;
    private AasRepository aasRepository;
    private SubmodelRegistry smRegistry;
    private SubmodelRepository smRepository;

    // Creator Properties:
    public Thread thread = new Thread(this);
    protected boolean stopped = false;
    protected int submodelCount = 0;
    protected int sleepInMs = 0;
    List<String> submodelIds = new LinkedList<>();
    SubmodelRemover submodelRemover;

    public TimeSeriesSubmodelCreator() {}

    public TimeSeriesSubmodelCreator(
            AasRegistry aasRegistry,
            AasRepository aasRepository,
            SubmodelRegistry smRegistry,
            SubmodelRepository smRepository,
            int submodelCount,
            int sleepInMs
    ) {
        this.aasRegistry = aasRegistry;
        this.aasRepository = aasRepository;
        this.smRegistry = smRegistry;
        this.smRepository = smRepository;
        this.submodelCount = submodelCount;
        this.sleepInMs = sleepInMs;
        this.submodelRemover = new SubmodelRemover(this, aasRepository, smRepository);
    }

    public void start() {
        thread.start();
        this.stopped = false;
    }

    public void stop() {
        this.stopped = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        if(submodelCount == 0) {
            System.out.println("Start registering submodels...");
            while (!this.stopped) {
                createShellWithTimeseriesSubmodel();
                sleep(sleepInMs);
            }
        } else
            createShellWithTimeseriesSubmodel(submodelCount);
    }

    abstract protected Submodel createShellWithTimeseriesSubmodel();

    abstract protected void createShellWithTimeseriesSubmodel(int count);

    protected void registerTriples(List<List<Object>> triples) {
        registerAasObjectsFromTriples(
                aasRegistry,
                aasRepository,
                smRegistry,
                smRepository,
                triples
        );
    }

    protected void sleep(int sleepInMs) {
        try {
            java.lang.Thread.sleep(sleepInMs);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
