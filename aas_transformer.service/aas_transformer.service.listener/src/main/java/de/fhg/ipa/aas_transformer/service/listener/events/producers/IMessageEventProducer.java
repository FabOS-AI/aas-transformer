package de.fhg.ipa.aas_transformer.service.listener.events.producers;

import de.fhg.ipa.aas_transformer.service.listener.events.MessageEvent;

import java.util.concurrent.LinkedBlockingQueue;

public interface IMessageEventProducer {

    LinkedBlockingQueue<? extends MessageEvent> getMessageEventCache();

}
