package io.jans.shibboleth.model.metadata.manual;

import java.util.Objects;

import io.jans.shibboleth.model.error.CannotBeNullOrBlank;
import io.jans.shibboleth.model.util.TrustResult;

public class SamlX509CertificateInfo implements CertificateInfo {
    
    private final String certificateData;

    private SamlX509CertificateInfo(String certificateData) {

        this.certificateData = certificateData;
    }

    @Override
    public boolean hasCertificateData() {

        return true;
    }

    @Override
    public TrustResult<String> getCertificateData() {

        return TrustResult.success(certificateData);
    }

    @Override
    public boolean equals(Object o) {

        if (o == this) return true;

        if (o == null ||  getClass() != o.getClass()) return false;

        SamlX509CertificateInfo other = (SamlX509CertificateInfo) o;
        return Objects.equals(certificateData,other.certificateData);
    }

    @Override
    public int hashCode() {

        return Objects.hash(certificateData);
    }

    public static TrustResult<CertificateInfo> fromBase64CertificateData(String certificateData) {

        if (certificateData == null) {

            return TrustResult.failure(CannotBeNullOrBlank.forField("certificateData"));
        }

        return TrustResult.success(new SamlX509CertificateInfo(certificateData));
    }
}
