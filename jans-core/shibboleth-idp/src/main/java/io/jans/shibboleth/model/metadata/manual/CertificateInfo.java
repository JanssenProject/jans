package io.jans.shibboleth.model.metadata.manual;

import io.jans.shibboleth.model.util.TrustResult;

public interface  CertificateInfo {

    public boolean hasCertificateData();
    public TrustResult<String>  getCertificateData();
}
