/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import java.io.FileInputStream;
import java.io.InputStream;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import io.jans.model.SmtpConfiguration;
import io.jans.model.SmtpConnectProtectionType;
import io.jans.util.StringHelper;
import io.jans.util.security.SecurityProviderUtility;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;

import org.slf4j.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import jakarta.activation.CommandMap;
import jakarta.activation.MailcapCommandMap;

import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.util.Date;
import java.util.Properties;

/**
 * Provides operations with sending E-mails
 *
 * @author Yuriy Movchan Date: 20/04/2014
 */
@RequestScoped
@Named
public class MailService {

    @Inject
    private Logger log;

    @Inject
    private SmtpConfiguration smtpConfiguration;

    private long connectionTimeout = 5000;
    
    private KeyStore keyStore = null;

    private PrivateKey privateKey = null;

    private X509Certificate[] x509Certificates = null;

    private boolean isReadyForSign = false;

    @PostConstruct
    public void init() {
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("message/rfc822;; x-java-content- handler=com.sun.mail.handlers.message_rfc822");

        try {
            String keystoreFile = smtpConfiguration.getKeyStore();
            String keystoreSecret = smtpConfiguration.getKeyStorePasswordDecrypted();
            String keystoreAlias = smtpConfiguration.getKeyStoreAlias();
            String keystoreSigningAlgorithm = smtpConfiguration.getSigningAlgorithm();

            if(keystoreFile == null || keystoreSecret == null || keystoreAlias == null || keystoreSigningAlgorithm == null) {
                return;
            }

            SecurityProviderUtility.KeyStorageType keystoreType = SecurityProviderUtility.solveKeyStorageType(keystoreFile);

            InputStream is = new FileInputStream(keystoreFile);

            switch (keystoreType) {
            case JKS_KS: {
                keyStore = KeyStore.getInstance("JKS");
                break;
            }
            case PKCS12_KS: {
                keyStore = KeyStore.getInstance("PKCS12", SecurityProviderUtility.getBCProvider());
                break;
            }
            case BCFKS_KS: {
                keyStore = KeyStore.getInstance("BCFKS", SecurityProviderUtility.getBCProvider());
                break;
            }
            }
            keyStore.load(is, keystoreSecret.toCharArray());
            Certificate[] certificates = null;
            privateKey = (PrivateKey)keyStore.getKey(keystoreAlias, keystoreSecret.toCharArray());
            certificates = keyStore.getCertificateChain(keystoreAlias);
            if (certificates != null) {
                x509Certificates = new X509Certificate[certificates.length];
                for (int i = 0; i < certificates.length; i++) {
                    x509Certificates[i] = (X509Certificate)certificates[i];
                }
            }
            isReadyForSign = (privateKey != null && x509Certificates != null && keystoreSigningAlgorithm != null);
        }
        catch (Exception ex) {
            isReadyForSign = false;
            log.error(ex.getMessage(), ex);
        }
    }
    
    public boolean sendMail(String to, String subject, String body) {
        String from = smtpConfiguration.getFromEmailAddress();
        return sendMail(from, from, to, to, subject, body, "");
    }

    public boolean sendMail(String from, String fromDisplayName, String to, String toDisplayName, String subject, String message, String htmlMessage) {
        return sendMail(from, fromDisplayName, to, null, subject, message, htmlMessage, false);
    }

    public boolean sendMailSigned(String from, String fromDisplayName, String to, String toDisplayName, String subject, String message, String htmlMessage) {
        return sendMail(from, fromDisplayName, to, null, subject, message, htmlMessage, true);
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    private boolean sendMail(String from, String fromDisplayName, String to, String toDisplayName, String subject,
            String message, String htmlMessage, boolean signMessage) {

        if (smtpConfiguration == null) {
            log.error("Failed to send message from '{}' to '{}' because the SMTP configuration isn't valid!", from, to);
            return false;
        }

        log.debug("Host name: " + smtpConfiguration.getHost() + ", port: " + smtpConfiguration.getPort() + ", connection time out: "
                + this.connectionTimeout);

        String mailFrom = from;
        if (StringHelper.isEmpty(mailFrom)) {
            mailFrom = smtpConfiguration.getFromEmailAddress();
        }

        String mailFromName = fromDisplayName;
        if (StringHelper.isEmpty(mailFromName)) {
            mailFromName = smtpConfiguration.getFromName();
        }

        Properties props = new Properties();

        props.put("mail.from", mailFrom);

        SmtpConnectProtectionType smtpConnectProtect = smtpConfiguration.getConnectProtection();

        if (smtpConnectProtect == SmtpConnectProtectionType.START_TLS) {
            props.put("mail.transport.protocol", "smtp");

            props.put("mail.smtp.host", smtpConfiguration.getHost());
            props.put("mail.smtp.port", smtpConfiguration.getPort());
            props.put("mail.smtp.connectiontimeout", this.connectionTimeout);
            props.put("mail.smtp.timeout", this.connectionTimeout);

            props.put("mail.smtp.socketFactory.class", "com.sun.mail.util.MailSSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", smtpConfiguration.getPort());
            if (smtpConfiguration.isServerTrust()) {
                props.put("mail.smtp.ssl.trust", smtpConfiguration.getHost());
            }
            props.put("mail.smtp.starttls.enable", true);
            props.put("mail.smtp.starttls.required", true);
        }
        else if (smtpConnectProtect == SmtpConnectProtectionType.SSL_TLS) {
            props.put("mail.transport.protocol.rfc822", "smtps");

            props.put("mail.smtps.host", smtpConfiguration.getHost());
            props.put("mail.smtps.port", smtpConfiguration.getPort());
            props.put("mail.smtps.connectiontimeout", this.connectionTimeout);
            props.put("mail.smtps.timeout", this.connectionTimeout);

            props.put("mail.smtp.socketFactory.class", "com.sun.mail.util.MailSSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", smtpConfiguration.getPort());
            if (smtpConfiguration.isServerTrust()) {
                props.put("mail.smtp.ssl.trust", smtpConfiguration.getHost());
            }
            props.put("mail.smtp.ssl.enable", true);
        } 
        else {
            props.put("mail.transport.protocol", "smtp");

            props.put("mail.smtp.host", smtpConfiguration.getHost());
            props.put("mail.smtp.port", smtpConfiguration.getPort());
            props.put("mail.smtp.connectiontimeout", this.connectionTimeout);
            props.put("mail.smtp.timeout", this.connectionTimeout);
        }

        Session session = null;
        if (smtpConfiguration.isRequiresAuthentication()) {
            
            if (smtpConnectProtect == SmtpConnectProtectionType.SSL_TLS) {
                props.put("mail.smtps.auth", "true");
            }
            else {
                props.put("mail.smtp.auth", "true");
            }

            final String userName = smtpConfiguration.getSmtpAuthenticationAccountUsername();
            final String password = smtpConfiguration.getSmtpAuthenticationAccountPasswordDecrypted();

            session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, password);
                }
            });
        }
        else {
            session = Session.getInstance(props, null);
        }

        MimeMessage msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(mailFrom, mailFromName));
            if (StringHelper.isEmpty(toDisplayName)) {
                msg.setRecipients(Message.RecipientType.TO, to);
            }
            else {
                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to, toDisplayName));
            }
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());

            if (StringHelper.isEmpty(htmlMessage)) {
                msg.setText(message + "\n", "UTF-8", "plain");
            } 
            else {
                // Unformatted text version
                final MimeBodyPart textPart = new MimeBodyPart();
                textPart.setText(message, "UTF-8", "plain");
                // HTML version
                final MimeBodyPart htmlPart = new MimeBodyPart();
                htmlPart.setText(htmlMessage, "UTF-8", "html");

                // Create the Multipart. Add BodyParts to it.
                final Multipart mp = new MimeMultipart("alternative");
                mp.addBodyPart(textPart);
                mp.addBodyPart(htmlPart);

                // Set Multipart as the message's content
                msg.setContent(mp);

                String signingAlgorithm = smtpConfiguration.getSigningAlgorithm();

                if (signMessage && isReadyForSign) {
                    MimeMultipart multiPart = createMultipartWithSignature(privateKey, x509Certificates, signingAlgorithm, msg);
                    msg.setContent(multiPart);
                }
            }
            Transport.send(msg);
        } catch (Exception ex) {
            log.error("Failed to send message", ex);
            return false;
        }

        return true;
    }

    /**
     * 
     * @param cert
     * @return
     * @throws CertificateParsingException
     */
    private static ASN1EncodableVector generateSignedAttributes(X509Certificate cert) throws CertificateParsingException {
        ASN1EncodableVector signedAttrs = new ASN1EncodableVector();
        SMIMECapabilityVector caps = new SMIMECapabilityVector();
        caps.addCapability(SMIMECapability.aES256_CBC);
        caps.addCapability(SMIMECapability.dES_EDE3_CBC);
        caps.addCapability(SMIMECapability.rC2_CBC, 128);
        signedAttrs.add(new SMIMECapabilitiesAttribute(caps));
        signedAttrs.add(new SMIMEEncryptionKeyPreferenceAttribute(SMIMEUtil.createIssuerAndSerialNumberFor(cert)));
        return signedAttrs;
    }

    /**
     * 
     * @param key
     * @param cert
     * @param signingAlgorithm
     * @param mm
     * @return
     * @throws CertificateEncodingException
     * @throws CertificateParsingException
     * @throws OperatorCreationException
     * @throws SMIMEException
     */
    public static MimeMultipart createMultipartWithSignature(PrivateKey key, X509Certificate cert, String signingAlgorithm, MimeMessage mm) throws CertificateEncodingException, CertificateParsingException, OperatorCreationException, SMIMEException {
        List<X509Certificate> certList = new ArrayList<X509Certificate>();
        certList.add(cert);

        JcaCertStore certs = new JcaCertStore(certList);
        ASN1EncodableVector signedAttrs = generateSignedAttributes(cert);

        SMIMESignedGenerator gen = new SMIMESignedGenerator();

        if (signingAlgorithm == null || signingAlgorithm.isEmpty()) {
            signingAlgorithm = cert.getSigAlgName();
        }

        gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().setProvider(SecurityProviderUtility.getBCProvider()).setSignedAttributeGenerator(new AttributeTable(signedAttrs)).build(signingAlgorithm, key, cert));        

        gen.addCertificates(certs);

        return gen.generate(mm);
    }

    /**
     * 
     * @param key
     * @param inCerts
     * @param signingAlgorithm
     * @param mm
     * @return
     * @throws CertificateEncodingException
     * @throws CertificateParsingException
     * @throws OperatorCreationException
     * @throws SMIMEException
     */
    public static MimeMultipart createMultipartWithSignature(PrivateKey key, X509Certificate[] inCerts, String signingAlgorithm, MimeMessage mm) throws CertificateEncodingException, CertificateParsingException, OperatorCreationException, SMIMEException {

        JcaCertStore certs = new JcaCertStore(Arrays.asList(inCerts));
        ASN1EncodableVector signedAttrs = generateSignedAttributes((X509Certificate)inCerts[0]);

        SMIMESignedGenerator gen = new SMIMESignedGenerator();

        if (signingAlgorithm == null || signingAlgorithm.isEmpty()) {
            signingAlgorithm = ((X509Certificate)inCerts[0]).getSigAlgName();
        }

        gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().setProvider(SecurityProviderUtility.getBCProvider()).setSignedAttributeGenerator(new AttributeTable(signedAttrs)).build(signingAlgorithm, key, (X509Certificate)inCerts[0]));        

        gen.addCertificates(certs);

        return gen.generate(mm);
    }    
    
}
