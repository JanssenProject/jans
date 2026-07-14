package io.jans.shibboleth.trust.config.metadata.manual;

import java.util.Objects;

import io.jans.shibboleth.trust.config.error.CannotBeNullOrBlank;
import io.jans.shibboleth.trust.shared.Result;

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
    public Result<String> getCertificateData() {

        return Result.success(certificateData);
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

    public static Result<CertificateInfo> fromBase64CertificateData(String certificateData) {

        if (certificateData == null) {

            return Result.failure(CannotBeNullOrBlank.forField("certificateData"));
        }

        return Result.success(new SamlX509CertificateInfo(certificateData));
    }
}
