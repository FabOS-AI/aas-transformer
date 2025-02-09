package de.fhg.ipa.aas_transformer.test.utils.extentions;

import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.containers.GenericContainer;

import java.util.ArrayList;
import java.util.List;

public class AbstractExtension extends SpringExtension {
    protected void startContainer(List<GenericContainer> containers) {
        List<Thread> threads = new ArrayList<>();

        containers.forEach(container -> {
            threads.add(new Thread(new ContainerStartThread(container)));
        });

        threads.forEach(Thread::start);
        threads.forEach(th -> {
            try {
                th.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }
}
