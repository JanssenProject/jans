package io.jans.shibboleth.trust.config.metadata.manual;

import io.jans.shibboleth.trust.config.error.UnsupportedOperation;
import io.jans.shibboleth.trust.shared.Result;

public class NoCertificateInfo implements CertificateInfo {
    
    public NoCertificateInfo() { }
    
    @Override
    public boolean hasCertificateData() {

        return false;
    }

    @Override
    public Result<String> getCertificateData() {

        return Result.failure(UnsupportedOperation.withMessage(
            "Cannot get certificate data from an absent certificate"
        ));
    }
}
