package io.jans.shibboleth.trust.config.metadata.manual;

import io.jans.shibboleth.trust.config.error.UnsupportedOperation;
import io.jans.shibboleth.trust.config.util.TrustResult;

public class NoCertificateInfo implements CertificateInfo {
    
    public NoCertificateInfo() { }
    
    @Override
    public boolean hasCertificateData() {

        return false;
    }

    @Override
    public TrustResult<String> getCertificateData() {

        return TrustResult.failure(UnsupportedOperation.withMessage(
            "Cannot get certificate data from an absent certificate"
        ));
    }
}
