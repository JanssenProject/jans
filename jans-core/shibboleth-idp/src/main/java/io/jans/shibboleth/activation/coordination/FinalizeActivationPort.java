package io.jans.shibboleth.activation.coordination;

import io.jans.shibboleth.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.config.diagnostics.ActivationDiagnostics;

@FunctionalInterface
public interface FinalizeActivationPort {

    void finalizeActivation(TrustRelationshipRef trustRelationshipId, ActivationDiagnostics diagnostics);
}
