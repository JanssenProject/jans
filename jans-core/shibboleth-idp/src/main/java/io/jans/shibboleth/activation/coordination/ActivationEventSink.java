package io.jans.shibboleth.activation.coordination;

@FunctionalInterface
public interface ActivationEventSink {

    void emit(ActivationEvent event);
}
