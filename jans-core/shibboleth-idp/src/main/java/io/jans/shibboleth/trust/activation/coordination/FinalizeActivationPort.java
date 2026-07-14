package io.jans.shibboleth.trust.activation.coordination;

import io.jans.shibboleth.trust.activation.model.TrustRelationshipRef;
import io.jans.shibboleth.trust.shared.diagnostics.ActivationDiagnostics;

@FunctionalInterface
public interface FinalizeActivationPort {

    void finalizeActivation(TrustRelationshipRef trustRelationshipId, ActivationDiagnostics diagnostics);
}
