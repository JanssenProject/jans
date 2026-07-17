package io.jans.shibboleth.trust.config.metadata.manual;

import io.jans.shibboleth.trust.shared.Result;

public interface  CertificateInfo {

    public boolean hasCertificateData();
    public Result<String>  getCertificateData();
}
