package io.jans.shibboleth.trust.config.metadata.manual;

import io.jans.shibboleth.trust.config.error.UnsupportedOperation;
import io.jans.kernel.Result;

public final class NoCertificateInfo implements CertificateInfo {
    
    public NoCertificateInfo() { 

        /**
         * empty constructor. The NoCertificateInfo class simply represents 
         * manual metadata with no certificate info. This is in order to avoid 
         * passing null , which will blur the intent we're trying to communicate
        */
    }
    
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

    @Override
    public boolean equals(Object o) {

        return this == o || (o != null && getClass() == o.getClass());
    }

    @Override
    public int hashCode() {

        return NoCertificateInfo.class.hashCode();
    }
}
