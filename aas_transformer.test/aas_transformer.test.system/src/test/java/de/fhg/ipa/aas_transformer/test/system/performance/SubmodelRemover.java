package de.fhg.ipa.aas_transformer.test.system.performance;

import de.fhg.ipa.aas_transformer.aas.AasRepository;
import de.fhg.ipa.aas_transformer.aas.SubmodelRepository;
import org.eclipse.digitaltwin.aas4j.v3.model.Reference;

import java.util.List;

import static java.lang.Thread.sleep;

public class SubmodelRemover implements Runnable {
    private final AasRepository aasRepository;
    private final SubmodelRepository smRepository;
    Thread thread = new Thread(this);
    boolean stopped = false;
    TimeSeriesSubmodelCreator creator;

    public SubmodelRemover(
            TimeSeriesSubmodelCreator creator,
            AasRepository aasRepository,
            SubmodelRepository smRepository
    ) {
        this.creator = creator;
        this.aasRepository = aasRepository;
        this.smRepository = smRepository;
    }

    public void start() {
        thread.start();
        stopped = false;
    }

    public void stop() {
        stopped = true;
        try {
            thread.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        while(!stopped) {
            if (creator.submodelIds.size() > 0) {
                String submodelId = creator.submodelIds.remove(0);
                aasRepository.getAllAasContainingSubmodelBySubmodelId(submodelId).forEach(aas -> {
                    List<Reference> submodelRefs = aas.getSubmodels();
                    while(submodelRefs.size() < 2) {
                        try {
                            sleep(10);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        submodelRefs = aasRepository.getAas(aas.getId()).getSubmodels();
                    }
                    submodelRefs.forEach(submodel -> {
                        String smId = submodel.getKeys().get(0).getValue();
                        try {
                            smRepository.deleteSubmodel(smId);
                        } catch (Exception e) {
                            System.out.println("Error deleting submodel: " + smId);
                        }
                    });

                });
            }
            try {
                sleep(creator.sleepInMs*2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
