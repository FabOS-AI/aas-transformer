package de.fhg.ipa.aas_transformer.test.utils.extentions;

import org.testcontainers.containers.GenericContainer;

public class ContainerStartThread implements Runnable{
    GenericContainer container;

    public ContainerStartThread(GenericContainer container) {
        this.container = container;
    }

    @Override
    public void run() {
        container.start();
    }
}
