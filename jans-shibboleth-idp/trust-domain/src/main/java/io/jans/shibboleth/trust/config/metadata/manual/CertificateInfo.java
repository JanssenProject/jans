package io.jans.shibboleth.trust.config.metadata.manual;

import io.jans.kernel.Result;

public sealed interface CertificateInfo permits
    NoCertificateInfo,
    SamlX509CertificateInfo {

    public boolean hasCertificateData();
    public Result<String>  getCertificateData();
}
