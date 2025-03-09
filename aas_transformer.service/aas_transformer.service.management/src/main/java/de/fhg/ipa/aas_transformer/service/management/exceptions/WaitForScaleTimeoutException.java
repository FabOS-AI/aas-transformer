package de.fhg.ipa.aas_transformer.service.management.exceptions;

public class WaitForScaleTimeoutException extends Exception {
    public WaitForScaleTimeoutException(String message) {
        super(message);
    }
}
