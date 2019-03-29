/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.net;

import java.io.Serializable;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Date;

import javax.net.ssl.X509TrustManager;

public final class TrustAllTrustManager implements X509TrustManager, Serializable {

    private static final long serialVersionUID = 1064131642204355149L;

    /**
     * Indicates whether to automatically trust expired or not-yet-valid
     * certificates.
     */
    private final boolean examineValidityDates;

    /**
     * Creates a new instance of this trust all trust manager that will trust any
     * certificate, including certificates that are expired or not yet valid.
     */
    public TrustAllTrustManager() {
        examineValidityDates = false;
    }

    /**
     * Creates a new instance of this trust all trust manager that will trust any
     * certificate, potentially excluding certificates that are expired or not yet
     * valid.
     *
     * @param examineValidityDates
     *            Indicates whether to reject certificates if the current time is
     *            outside the validity window for the certificate.
     */
    public TrustAllTrustManager(final boolean examineValidityDates) {
        this.examineValidityDates = examineValidityDates;
    }

    /**
     * Indicate whether to reject certificates if the current time is outside the
     * validity window for the certificate.
     *
     * @return {@code true} if the certificate validity time should be examined and
     *         certificates should be rejected if they are expired or not yet valid,
     *         or {@code false} if certificates should be accepted even outside of
     *         the validity window.
     */
    public boolean examineValidityDates() {
        return examineValidityDates;
    }

    /**
     * Checks to determine whether the provided client certificate chain should be
     * trusted. A certificate will only be rejected (by throwing a
     * {@link CertificateException}) if certificate validity dates should be
     * examined and the certificate or any of its issuers is outside of the validity
     * window.
     *
     * @param chain
     *            The client certificate chain for which to make the determination.
     * @param authType
     *            The authentication type based on the client certificate.
     *
     * @throws CertificateException
     *             If the provided client certificate chain should not be trusted.
     */
    public void checkClientTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        if (examineValidityDates) {
            final Date currentDate = new Date();

            for (final X509Certificate c : chain) {
                c.checkValidity(currentDate);
            }
        }
    }

    /**
     * Checks to determine whether the provided server certificate chain should be
     * trusted. A certificate will only be rejected (by throwing a
     * {@link CertificateException}) if certificate validity dates should be
     * examined and the certificate or any of its issuers is outside of the validity
     * window.
     *
     * @param chain
     *            The server certificate chain for which to make the determination.
     * @param authType
     *            The key exchange algorithm used.
     *
     * @throws CertificateException
     *             If the provided server certificate chain should not be trusted.
     */
    public void checkServerTrusted(final X509Certificate[] chain, final String authType) throws CertificateException {
        if (examineValidityDates) {
            final Date currentDate = new Date();

            for (final X509Certificate c : chain) {
                c.checkValidity(currentDate);
            }
        }
    }

    /**
     * Retrieves the accepted issuer certificates for this trust manager. This will
     * always return an empty array.
     *
     * @return The accepted issuer certificates for this trust manager.
     */
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }
}
