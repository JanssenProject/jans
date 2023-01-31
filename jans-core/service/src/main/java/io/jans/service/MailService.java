/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;
import javax.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.commons.io.FilenameUtils;

import org.bouncycastle.asn1.ASN1EncodableVector;
import org.bouncycastle.asn1.cms.AttributeTable;
import org.bouncycastle.asn1.smime.SMIMECapabilitiesAttribute;
import org.bouncycastle.asn1.smime.SMIMECapability;
import org.bouncycastle.asn1.smime.SMIMECapabilityVector;
import org.bouncycastle.asn1.smime.SMIMEEncryptionKeyPreferenceAttribute;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoGeneratorBuilder;
import org.bouncycastle.mail.smime.SMIMEException;
import org.bouncycastle.mail.smime.SMIMESignedGenerator;
import org.bouncycastle.mail.smime.SMIMEUtil;
import org.bouncycastle.operator.OperatorCreationException;

import io.jans.model.SmtpConfiguration;
import io.jans.model.SmtpConnectProtectionType;
import io.jans.util.StringHelper;
//import io.jans.as.model.util.SecurityProviderUtility;
import io.jans.util.security.SecurityProviderUtility;

import org.slf4j.Logger;

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

    private KeyStore keyStore;

    private long connectionTimeout = 5000;

    @PostConstruct
    public void init() {
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap();
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html");
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml");
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain");
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed");
        mc.addMailcap("message/rfc822;; x-java-content- handler=com.sun.mail.handlers.message_rfc822");

        String keystoreFile = smtpConfiguration.getKeyStore();
        String keystoreSecret = smtpConfiguration.getKeyStorePasswordDecrypted();

        SecurityProviderUtility.KeyStorageType keystoreType = solveKeyStorageType(keystoreFile);

        try(InputStream is = new FileInputStream(keystoreFile)) {
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
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public boolean sendMail(String to, String subject, String message) {
        return sendMail(smtpConfiguration, null, null, to, null, subject, message, null);
    }

    public boolean sendMail(String to, String toDisplayName, String subject, String message, String htmlMessage) {
        return sendMail(smtpConfiguration, null, null, to, null, subject, message, htmlMessage);
    }

    public boolean sendMail(String from, String fromDisplayName, String to, String toDisplayName, String subject,
            String message, String htmlMessage) {
        return sendMail(smtpConfiguration, from, fromDisplayName, to, null, subject, message, htmlMessage);
    }

    public boolean sendMail(SmtpConfiguration mailSmtpConfiguration, String from, String fromDisplayName, String to,
            String toDisplayName,
            String subject, String message, String htmlMessage) {
        if (mailSmtpConfiguration == null) {
            log.error("Failed to send message from '{}' to '{}' because the SMTP configuration isn't valid!", from, to);
            return false;
        }

        log.debug("Host name: " + mailSmtpConfiguration.getHost() + ", port: " + mailSmtpConfiguration.getPort() + ", connection time out: "
                + this.connectionTimeout);

        String mailFrom = from;
        if (StringHelper.isEmpty(mailFrom)) {
            mailFrom = mailSmtpConfiguration.getFromEmailAddress();
        }

        String mailFromName = fromDisplayName;
        if (StringHelper.isEmpty(mailFromName)) {
            mailFromName = mailSmtpConfiguration.getFromName();
        }

        Properties props = new Properties();
        props.put("mail.smtp.host", mailSmtpConfiguration.getHost());
        props.put("mail.smtp.port", mailSmtpConfiguration.getPort());
        props.put("mail.from", mailFrom);
        props.put("mail.smtp.connectiontimeout", this.connectionTimeout);
        props.put("mail.smtp.timeout", this.connectionTimeout);
        props.put("mail.transport.protocol", "smtp");

        SmtpConnectProtectionType smtpConnectProtect = mailSmtpConfiguration.getConnectProtection();

        if (smtpConnectProtect == SmtpConnectProtectionType.START_TLS) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", mailSmtpConfiguration.getPort());
            props.put("mail.smtp.ssl.trust", mailSmtpConfiguration.getHost());
            props.put("mail.smtp.starttls.enable", true);
        }
        else if (smtpConnectProtect == SmtpConnectProtectionType.SSL_TLS) {
            props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", mailSmtpConfiguration.getPort());
            props.put("mail.smtp.ssl.trust", mailSmtpConfiguration.getHost());
            props.put("mail.smtp.ssl.enable", true);
        }

        Session session = null;
        if (mailSmtpConfiguration.isRequiresAuthentication()) {
            props.put("mail.smtp.auth", "true");

            final String userName = mailSmtpConfiguration.getUserName();
            final String password = mailSmtpConfiguration.getPasswordDecrypted();

            session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, password);
                }
            });
        } else {
            session = Session.getInstance(props, null);
        }

        MimeMessage msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(mailFrom, mailFromName));
            if (StringHelper.isEmpty(toDisplayName)) {
                msg.setRecipients(Message.RecipientType.TO, to);
            } else {
                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to, toDisplayName));
            }
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());

            if (StringHelper.isEmpty(htmlMessage)) {
                msg.setText(message + "\n", "UTF-8", "plain");
            } else {
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
            }

            Transport.send(msg);
        } catch (Exception ex) {
            log.error("Failed to send message", ex);
            return false;
        }

        return true;
    }

    public boolean sendMailSigned(String to, String subject, String message) {
        return sendMailSigned(smtpConfiguration, null, null, to, null, subject, message, null);
    }

    public boolean sendMailSigned(String to, String toDisplayName, String subject, String message, String htmlMessage) {
        return sendMailSigned(smtpConfiguration, null, null, to, null, subject, message, htmlMessage);
    }

    public boolean sendMailSigned(String from, String fromDisplayName, String to, String toDisplayName, String subject,
            String message, String htmlMessage) {
        return sendMailSigned(smtpConfiguration, from, fromDisplayName, to, null, subject, message, htmlMessage);
    }

    public boolean sendMailSigned(SmtpConfiguration mailSmtpConfiguration, String from, String fromDisplayName, String to,
            String toDisplayName,
            String subject, String message, String htmlMessage) {
        if (mailSmtpConfiguration == null) {
            log.error("Failed to send message from '{}' to '{}' because the SMTP configuration isn't valid!", from, to);
            return false;
        }

        log.debug("Host name: " + mailSmtpConfiguration.getHost() + ", port: " + mailSmtpConfiguration.getPort() + ", connection time out: "
                + this.connectionTimeout);

        PrivateKey privateKey = null;

        Certificate[] certificates = null;
        X509Certificate[] x509Certificates = null;

        try {
            privateKey = (PrivateKey)keyStore.getKey(mailSmtpConfiguration.getKeyStoreAlias(),
                    smtpConfiguration.getKeyStorePasswordDecrypted().toCharArray());
            certificates = keyStore.getCertificateChain(mailSmtpConfiguration.getKeyStoreAlias());
            if (certificates != null) {
                x509Certificates = new X509Certificate[certificates.length];
                for (int i = 0; i < certificates.length; i++) {
                    x509Certificates[i] = (X509Certificate)certificates[i];
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        String mailFrom = from;
        if (StringHelper.isEmpty(mailFrom)) {
            mailFrom = mailSmtpConfiguration.getFromEmailAddress();
        }

        String mailFromName = fromDisplayName;
        if (StringHelper.isEmpty(mailFromName)) {
            mailFromName = mailSmtpConfiguration.getFromName();
        }

        Properties props = new Properties();

        props.put("mail.from", mailFrom);

        SmtpConnectProtectionType smtpConnectProtect = mailSmtpConfiguration.getConnectProtection();

        if (smtpConnectProtect == SmtpConnectProtectionType.START_TLS) {
            props.put("mail.transport.protocol", "smtp");

            props.put("mail.smtp.host", mailSmtpConfiguration.getHost());
            props.put("mail.smtp.port", mailSmtpConfiguration.getPort());
            props.put("mail.smtp.connectiontimeout", this.connectionTimeout);
            props.put("mail.smtp.timeout", this.connectionTimeout);

            props.put("mail.smtp.socketFactory.class", "com.sun.mail.util.MailSSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", mailSmtpConfiguration.getPort());
            if (mailSmtpConfiguration.isServerTrust()) {
                props.put("mail.smtp.ssl.trust", mailSmtpConfiguration.getHost());
            }
            props.put("mail.smtp.starttls.enable", true);
            props.put("mail.smtp.starttls.required", true);
        }
        else if (smtpConnectProtect == SmtpConnectProtectionType.SSL_TLS) {
            props.put("mail.transport.protocol.rfc822", "smtps");

            props.put("mail.smtps.host", mailSmtpConfiguration.getHost());
            props.put("mail.smtps.port", mailSmtpConfiguration.getPort());
            props.put("mail.smtps.connectiontimeout", this.connectionTimeout);
            props.put("mail.smtps.timeout", this.connectionTimeout);

            props.put("mail.smtp.socketFactory.class", "com.sun.mail.util.MailSSLSocketFactory");
            props.put("mail.smtp.socketFactory.port", mailSmtpConfiguration.getPort());
            if (mailSmtpConfiguration.isServerTrust()) {
                props.put("mail.smtp.ssl.trust", mailSmtpConfiguration.getHost());
            }
            props.put("mail.smtp.ssl.enable", true);
        } 
        else {
            props.put("mail.transport.protocol", "smtp");

            props.put("mail.smtp.host", mailSmtpConfiguration.getHost());
            props.put("mail.smtp.port", mailSmtpConfiguration.getPort());
            props.put("mail.smtp.connectiontimeout", this.connectionTimeout);
            props.put("mail.smtp.timeout", this.connectionTimeout);
        }

        Session session = null;
        if (mailSmtpConfiguration.isRequiresAuthentication()) {
            
            if (smtpConnectProtect == SmtpConnectProtectionType.SSL_TLS) {
                props.put("mail.smtps.auth", "true");
            }
            else {
                props.put("mail.smtp.auth", "true");
            }

            final String userName = mailSmtpConfiguration.getUserName();
            final String password = mailSmtpConfiguration.getPasswordDecrypted();

            session = Session.getInstance(props, new javax.mail.Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, password);
                }
            });
        } else {
            session = Session.getInstance(props, null);
        }

        MimeMessage msg = new MimeMessage(session);
        try {
            msg.setFrom(new InternetAddress(mailFrom, mailFromName));
            if (StringHelper.isEmpty(toDisplayName)) {
                msg.setRecipients(Message.RecipientType.TO, to);
            } else {
                msg.addRecipient(Message.RecipientType.TO, new InternetAddress(to, toDisplayName));
            }
            msg.setSubject(subject, "UTF-8");
            msg.setSentDate(new Date());

            if (StringHelper.isEmpty(htmlMessage)) {
                msg.setText(message + "\n", "UTF-8", "plain");
            } else {
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

                MimeMultipart multiPart = createMultipartWithSignature(privateKey, x509Certificates, smtpConfiguration.getSigningAlgorithm(), msg);                

                msg.setContent(multiPart);
            }

            Transport.send(msg);
        } catch (Exception ex) {
            log.error("Failed to send message", ex);
            return false;
        }

        return true;
    }

    public long getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(long connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
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
     * @param dataPart
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

    /**
     * 
     * @return
     */
    private SecurityProviderUtility.KeyStorageType solveKeyStorageType(final String keyStoreFile) {
        SecurityProviderUtility.SecurityModeType securityMode = SecurityProviderUtility.getSecurityMode();
        if (securityMode == null) {
            throw new InvalidParameterException("Security Mode wasn't initialized. Call installBCProvider() before");
        }
        String keyStoreExt = FilenameUtils.getExtension(keyStoreFile);
        SecurityProviderUtility.KeyStorageType keyStorageType = SecurityProviderUtility.KeyStorageType.fromExtension(keyStoreExt);
        boolean ksTypeFound = false;
        for (SecurityProviderUtility.KeyStorageType ksType : securityMode.getKeystorageTypes()) {
            if (keyStorageType == ksType) {
                ksTypeFound = true;
                break;
            }
        }
        if (!ksTypeFound) {
            switch (securityMode) {
            case BCFIPS_SECURITY_MODE: {
                keyStorageType =  SecurityProviderUtility.KeyStorageType.BCFKS_KS;
                break;
            }
            case BCPROV_SECURITY_MODE: {
                keyStorageType = SecurityProviderUtility.KeyStorageType.PKCS12_KS;
                break;
            }
            }
        }
        return keyStorageType;
    }

}
