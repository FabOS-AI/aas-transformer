package de.fhg.ipa.aas_transformer.test.system.performance;


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

public class TimeSeriesSubmodelCreator implements Runnable {
    private AasRegistry aasRegistry;
    private AasRepository aasRepository;
    private SubmodelRegistry smRegistry;
    private SubmodelRepository smRepository;
    Thread thread = new Thread(this);
    boolean stopped = false;
    int submodelCount = 0;
    int sleepInMs = 0;
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

    public void stop() throws InterruptedException {
        this.stopped = true;
        thread.join();
    }

    @Override
    public void run() {
        if(submodelCount == 0) {
            System.out.println("Start registering submodels...");
            while(!this.stopped) {
                List<List<Object>> triples = getRandomTimeseriesTriples(1);
                registerAasObjectsFromTriples(
                        aasRegistry,
                        aasRepository,
                        smRegistry,
                        smRepository,
                        triples
                );
                submodelIds.add(
                        ((Submodel)triples.get(0).get(1)).getId()
                );
                try {
                    sleep(sleepInMs);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        } else {
            List<List<Object>> triples = getRandomTimeseriesTriples(submodelCount);
            System.out.println("Start registering " + submodelCount + " submodels");
            registerAasObjectsFromTriples(
                    aasRegistry,
                    aasRepository,
                    smRegistry,
                    smRepository,
                    triples
            );
        }
    }
}
