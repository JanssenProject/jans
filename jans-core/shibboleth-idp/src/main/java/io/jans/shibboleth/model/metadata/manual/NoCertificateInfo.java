package io.jans.shibboleth.model.metadata.manual;

import io.jans.shibboleth.model.error.UnsupportedOperation;
import io.jans.shibboleth.model.util.TrustResult;

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
