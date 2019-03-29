/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.saml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.KeySpec;
import java.security.spec.PKCS8EncodedKeySpec;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.ws.security.saml.ext.OpenSAMLUtil;
import org.gluu.saml.exception.CloneFailedException;
import org.gluu.util.security.CertificateHelper;
import org.opensaml.xml.security.credential.BasicCredential;
import org.opensaml.xml.security.credential.Credential;
import org.opensaml.xml.security.credential.UsageType;

/**
 * Configuration settings
 *
 * @author Yuriy Movchan Date: 24/04/2014
 */
public class SamlConfiguration {

    private String idpSsoTargetUrl;
    private String assertionConsumerServiceUrl;
    private String issuer;
    private String nameIdentifierFormat;
    private X509Certificate certificate;
    private boolean useRequestedAuthnContext;

    private PrivateKey privateKey;
    private String sigAlg = "SHA256withRSA";
    private String sigAlgUrl = "http://www.w3.org/2001/04/xmldsig-more#rsa-sha256";

    public String getIdpSsoTargetUrl() {
        return idpSsoTargetUrl;
    }

    public void setIdpSsoTargetUrl(String idpSsoTargetUrl) {
        this.idpSsoTargetUrl = idpSsoTargetUrl;
    }

    public String getAssertionConsumerServiceUrl() {
        return assertionConsumerServiceUrl;
    }

    public void setAssertionConsumerServiceUrl(String assertionConsumerServiceUrl) {
        this.assertionConsumerServiceUrl = assertionConsumerServiceUrl;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getNameIdentifierFormat() {
        return nameIdentifierFormat;
    }

    public void setNameIdentifierFormat(String nameIdentifierFormat) {
        this.nameIdentifierFormat = nameIdentifierFormat;
    }

    public X509Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    public boolean isUseRequestedAuthnContext() {
        return useRequestedAuthnContext;
    }

    public void setUseRequestedAuthnContext(boolean useRequestedAuthnContext) {
        this.useRequestedAuthnContext = useRequestedAuthnContext;
    }

    public void loadCertificateFromString(String certificateString) throws CertificateException {
        this.certificate = CertificateHelper.loadCertificate(certificateString);
    }

    @Override
    public Object clone() throws CloneFailedException {
        try {
            return BeanUtils.cloneBean(this);
        } catch (Exception ex) {
            throw new CloneFailedException(ex);
        }
    }

    public String getSigAlg() {
        return sigAlg;
    }

    public void setSigAlg(String sigAlg) {
        this.sigAlg = sigAlg;
    }

    public String getSigAlgUrl() {
        return sigAlgUrl;
    }

    public void setSigAlgUrl(String sigAlgUrl) {
        this.sigAlgUrl = sigAlgUrl;
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(PrivateKey privateKey) {
        this.privateKey = privateKey;
    }

    /**
     * loads the private key for digital signature
     *
     * @param prvKeyPath
     *            file path to location of private key
     * @throws Exception
     */
    public void loadPrivateKey(String prvKeyPath) throws Exception {
        OpenSAMLUtil.initSamlEngine();

        File privKeyFile = new File(prvKeyPath);
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(new FileInputStream(privKeyFile));
        } catch (FileNotFoundException e) {
            throw new Exception("Could not locate keyfile at '" + prvKeyPath + "'", e);
        }
        byte[] privKeyBytes = new byte[(int) privKeyFile.length()];
        bis.read(privKeyBytes);
        bis.close();
        KeySpec ks = new PKCS8EncodedKeySpec(privKeyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        privateKey = keyFactory.generatePrivate(ks);
    }

    protected Credential getCredential() {
        BasicCredential credential = new BasicCredential();
        credential.setPublicKey(certificate.getPublicKey());
        credential.setPrivateKey(privateKey);
        credential.setUsageType(UsageType.SIGNING);
        return credential;
    }

}
