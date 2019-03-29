/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.util;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <p>
 * EasyX509TrustManager unlike default {@link X509TrustManager} accepts
 * self-signed certificates.
 * </p>
 * <p>
 * This trust manager SHOULD NOT be used for productive systems due to security
 * reasons, unless it is a concious decision and you are perfectly aware of
 * security implications of accepting self-signed certificates
 * </p>
 *
 * @author <a href="mailto:adrian.sutton@ephox.com">Adrian Sutton</a>
 * @author <a href="mailto:oleg@ural.ru">Oleg Kalnichevski</a>
 *
 *         <p>
 *         DISCLAIMER: HttpClient developers DO NOT actively support this
 *         component. The component is provided as a reference material, which
 *         may be inappropriate for use without additional customization.
 *         </p>
 */

public class EasyX509TrustManager implements X509TrustManager {
    private X509TrustManager standardTrustManager = null;

    /** Log object for this class. */
    private static final Log LOG = LogFactory.getLog(EasyX509TrustManager.class);

    /**
     * Constructor for EasyX509TrustManager.
     */
    public EasyX509TrustManager(KeyStore keystore) throws NoSuchAlgorithmException, KeyStoreException {
        super();
        TrustManagerFactory factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init(keystore);
        TrustManager[] trustmanagers = factory.getTrustManagers();
        if (trustmanagers.length == 0) {
            throw new NoSuchAlgorithmException("no trust manager found");
        }
        this.standardTrustManager = (X509TrustManager) trustmanagers[0];
    }

    /**
     * @see javax.net.ssl.X509TrustManager#checkClientTrusted(X509Certificate[],
     *      String authType)
     */
    public void checkClientTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
        standardTrustManager.checkClientTrusted(certificates, authType);
    }

    /**
     * @see javax.net.ssl.X509TrustManager#checkServerTrusted(X509Certificate[],
     *      String authType)
     */
    public void checkServerTrusted(X509Certificate[] certificates, String authType) throws CertificateException {
        if (certificates != null && LOG.isDebugEnabled()) {
            LOG.debug("Server certificate chain:");
            for (int i = 0; i < certificates.length; i++) {
                LOG.debug("X509Certificate[" + i + "]=" + certificates[i]);
            }
        }
        if (certificates != null && (certificates.length == 1)) {
            certificates[0].checkValidity();
        } else {
            List<X509Certificate> certs = new ArrayList<X509Certificate>();
            if (certificates != null) {
                certs.addAll(Arrays.asList(certificates));
            }
            X509Certificate certChain = certs.get(0);
            certs.remove(certChain);
            LinkedList<X509Certificate> chainList = new LinkedList<X509Certificate>();
            chainList.add(certChain);
            Principal certIssuer = certChain.getIssuerDN();
            Principal certSubject = certChain.getSubjectDN();
            while (!certs.isEmpty()) {
                List<X509Certificate> tempcerts = new ArrayList<X509Certificate>();
                tempcerts.addAll(certs);
                for (X509Certificate cert : tempcerts) {
                    if (cert.getIssuerDN().equals(certSubject)) {
                        chainList.addFirst(cert);
                        certSubject = cert.getSubjectDN();
                        certs.remove(cert);
                        continue;
                    }

                    if (cert.getSubjectDN().equals(certIssuer)) {
                        chainList.addLast(cert);
                        certIssuer = cert.getIssuerDN();
                        certs.remove(cert);
                        continue;
                    }
                }
            }
            standardTrustManager.checkServerTrusted(chainList.toArray(new X509Certificate[] {}), authType);

        }
    }

    /**
     * @see javax.net.ssl.X509TrustManager#getAcceptedIssuers()
     */
    public X509Certificate[] getAcceptedIssuers() {
        return this.standardTrustManager.getAcceptedIssuers();
    }
}
