package io.jans.shibboleth.trust.activation.coordination;

@FunctionalInterface
public interface ActivationEventSink {

    void emit(ActivationEvent event);
}
