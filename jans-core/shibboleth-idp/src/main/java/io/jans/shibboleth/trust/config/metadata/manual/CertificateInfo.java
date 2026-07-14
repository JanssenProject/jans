package io.jans.shibboleth.trust.config.metadata.manual;

import io.jans.shibboleth.trust.config.util.TrustResult;

public interface  CertificateInfo {

    public boolean hasCertificateData();
    public TrustResult<String>  getCertificateData();
}
