package io.jans.shibboleth.trust.config.metadata.manual;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link NoCertificateInfo} is a fieldless null-object, so all instances must be value-equal — otherwise
 * a MANUAL metadata source with no signing certificate fails value-equality (e.g. across a persistence
 * round-trip).
 */
@DisplayName("NoCertificateInfo — null-object value equality")
public class NoCertificateInfoTests {

    @Test
    @DisplayName("GIVEN two NoCertificateInfo instances THEN they are equal with equal hashCodes")
    public void instancesAreEqual() {

        NoCertificateInfo a = new NoCertificateInfo();
        NoCertificateInfo b = new NoCertificateInfo();

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("GIVEN a NoCertificateInfo and an X509 certificate THEN they are not equal")
    public void differsFromRealCertificate() {

        CertificateInfo x509 = SamlX509CertificateInfo
            .fromBase64CertificateData("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA").getValue();

        assertThat(new NoCertificateInfo()).isNotEqualTo(x509);
    }
}
