package io.jans.casa.plugins.emailotp;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.activation.CommandMap;
import javax.activation.MailcapCommandMap;

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
import io.jans.casa.credential.BasicCredential;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.emailotp.model.EmailPerson;
import io.jans.casa.plugins.emailotp.model.JansConfiguration;
import io.jans.casa.plugins.emailotp.model.SmtpConfiguration;
import io.jans.casa.plugins.emailotp.model.SmtpConnectProtectionType;
import io.jans.casa.plugins.emailotp.model.VerifiedEmail;
import io.jans.casa.service.IPersistenceService;

import io.jans.util.security.SecurityProviderUtility;
import io.jans.util.security.SecurityProviderUtility.SecurityModeType;
import io.jans.util.security.StringEncrypter.EncryptionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;


public class EmailOtpService {

    private static final Logger logger = LoggerFactory.getLogger(EmailOtpService.class);

    private static EmailOtpService singleInstance = null;

    public static final String ACR = "email_2fa_core";

    public static final String DEF_MAIL_FROM                        = "mail.from";
    public static final String DEF_MAIL_TRANSPORT_PROTOCOL          = "mail.transport.protocol";
    public static final String DEF_MAIL_SMTP_HOST                   = "mail.smtp.host";
    public static final String DEF_MAIL_SMTP_PORT                   = "mail.smtp.port";
    public static final String DEF_MAIL_SMTP_CONNECTION_TIMEOUT     = "mail.smtp.connectiontimeout";
    public static final String DEF_MAIL_SMTP_TIMEOUT                = "mail.smtp.timeout";
    public static final String DEF_MAIL_SMTP_SOCKET_FACTORY_CLASS   = "mail.smtp.socketFactory.class";
    public static final String DEF_MAIL_SMTP_SOCKET_FACTORY_PORT    = "mail.smtp.socketFactory.port";
    public static final String DEF_MAIL_SMTP_SSL_TRUST              = "mail.smtp.ssl.trust";
    public static final String DEF_MAIL_SMTP_STARTTLS_ENABLE        = "mail.smtp.starttls.enable";
    public static final String DEF_MAIL_SMTP_STARTTLS_REQUIRED      = "mail.smtp.starttls.required";
    public static final String DEF_MAIL_TRANSPORT_PROTOCOL_RFC822   = "mail.transport.protocol.rfc822";
    public static final String DEF_MAIL_SMTP_SSL_ENABLE             = "mail.smtp.ssl.enable";
    public static final String DEF_MAIL_SMTPS_AUTH                  = "mail.smtps.auth";
    public static final String DEF_MAIL_SMTP_AUTH                   = "mail.smtp.auth";
    public static final String DEF_MAIL_SMTPS_HOST                  = "mail.smtps.host";
    public static final String DEF_MAIL_SMTPS_PORT                  = "mail.smtps.port";
    public static final String DEF_MAIL_SMTPS_CONNECTION_TIMEOUT    = "mail.smtps.connectiontimeout";
    public static final String DEF_MAIL_SMTPS_TIMEOUT               = "mail.smtps.timeout";
    public static final String DEF_MAIL_SSL_SOCKET_FACTORY          = "com.sun.mail.util.MailSSLSocketFactory";

    private Map<String, String> properties;
    private IPersistenceService persistenceService;
    private ObjectMapper mapper;
    private long connectionTimeout = 5000;
    private KeyStore keyStore;

    static {
        SecurityProviderUtility.installBCProvider();
    }

    /**
     * 
     */
    private EmailOtpService() {
        MailcapCommandMap mc = (MailcapCommandMap) CommandMap.getDefaultCommandMap(); 
        mc.addMailcap("text/html;; x-java-content-handler=com.sun.mail.handlers.text_html"); 
        mc.addMailcap("text/xml;; x-java-content-handler=com.sun.mail.handlers.text_xml"); 
        mc.addMailcap("text/plain;; x-java-content-handler=com.sun.mail.handlers.text_plain"); 
        mc.addMailcap("multipart/*;; x-java-content-handler=com.sun.mail.handlers.multipart_mixed"); 
        mc.addMailcap("message/rfc822;; x-java-content- handler=com.sun.mail.handlers.message_rfc822");
    }

    /**
     * 
     * @return
     */
    public static EmailOtpService getInstance() {
        if (singleInstance == null) {
            synchronized (EmailOtpService.class) {
                singleInstance = new EmailOtpService();
            }
        }
        return singleInstance;
    }

    public void init() {
        try {
            persistenceService = Utils.managedBean(IPersistenceService.class);

            reloadConfiguration();
            mapper = new ObjectMapper();

            SmtpConfiguration smtpConfiguration = getConfiguration().getSmtpConfiguration();

            String keystoreFile = smtpConfiguration.getKeyStore();
            String keystoreSecret = decrypt(smtpConfiguration.getKeyStorePassword());

            SecurityProviderUtility.KeyStorageType keystoreType = solveKeyStorageType(keystoreFile);

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
            try (InputStream is = new FileInputStream(keystoreFile)) {
                keyStore.load(is, keystoreSecret.toCharArray());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 
     */
    public void reloadConfiguration() {
        ObjectMapper localMapper = new ObjectMapper();
        properties = persistenceService.getCustScriptConfigProperties(ACR);
        if (properties == null) {
            if (logger.isWarnEnabled()) { // according to Sonar request, as ACR.toUpperCase() is provided before checking    
                logger.warn(
                        "Config. properties for custom script '{}' could not be read. Features related to {} will not be accessible",
                        ACR, ACR.toUpperCase());
            }
        } else {
            try {
                if (logger.isInfoEnabled()) {
                    logger.info("Settings found were: {}", localMapper.writeValueAsString(properties));
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 
     * @param value
     * @return
     */
    public String getScriptPropertyValue(String value) {
        return properties.get(value);
    }

    /**
     * 
     * @param uniqueIdOfTheUser
     * @return
     */
    public List<BasicCredential> getCredentials(String uniqueIdOfTheUser) {
        List<VerifiedEmail> verifiedEmails = getVerifiedEmail(uniqueIdOfTheUser);
        List<BasicCredential> list = new ArrayList<>();
        for (VerifiedEmail v : verifiedEmails) {
            list.add(new BasicCredential(v.getEmail(), v.getAddedOn()));
        }
        return list;
    }

    /**
     * 
     * @param userId
     * @return
     */
    public List<VerifiedEmail> getVerifiedEmail(String userId) {
        List<VerifiedEmail> verifiedEmails = new ArrayList<>();
        try {
            EmailPerson testPerson = new EmailPerson();

            String searchMask = String.format("inum=%s,ou=people,o=jans", userId);
            testPerson.setBaseDn(searchMask);

            EmailPerson person = persistenceService.get(EmailPerson.class, persistenceService.getPersonDn(userId));
            String json = person.getJansEmail();
            json = Utils.isEmpty(json) ? "[]" : mapper.readTree(json).get("email-ids").toString();
            verifiedEmails = mapper.readValue(json, new TypeReference<List<VerifiedEmail>>() { });
            VerifiedEmail primaryMail = getExtraEmailId(person.getMail(), verifiedEmails);
            // implies that this has not been already added
            if (primaryMail != null) {
                updateEmailIdAdd(userId, verifiedEmails, primaryMail);
                verifiedEmails.add(primaryMail);

            }
            logger.info("getVerifiedEmail. User '{}' has {}", userId,
                    verifiedEmails.stream().map(VerifiedEmail::getEmail).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return verifiedEmails;
    }

    /**
     * 
     * @param uniqueIdOfTheUser
     * @return
     */
    public int getCredentialsTotal(String uniqueIdOfTheUser) {
        return getVerifiedEmail(uniqueIdOfTheUser).size();
    }

    /**
     * 
     * @return
     * @throws IOException
     */
    public JansConfiguration getConfiguration() throws IOException {
        String baseDN = "ou=configuration,o=jans";
        List<JansConfiguration> configRecs = persistenceService.find(JansConfiguration.class, baseDN, null, 0, -1);
        Optional<JansConfiguration> resConfg = configRecs.stream().filter(jansConf -> baseDN.equals(jansConf.getDn())).findFirst();
        if (!resConfg.isPresent()) {
            throw new IOException("JansConfiguration isn't found.");
        }
        return resConfg.get();
    }

    /**
     * 
     * @param emailId
     * @param subject
     * @param body
     * @param sign
     * @return
     * @throws IOException
     */
    public boolean sendEmailWithOTP(String emailId, String subject, String body, boolean sign) throws IOException {
        SmtpConfiguration smtpConfiguration = getConfiguration().getSmtpConfiguration();
        if (smtpConfiguration == null) {
            logger.error("Failed to send email. SMTP settings not found. Please configure SMTP settings in Janssen");
            return false;
        }

        Properties props = new Properties();

        props.put(DEF_MAIL_FROM, "Gluu Casa");

        SmtpConnectProtectionType smtpConnectProtect = smtpConfiguration.getConnectProtection();

        if (smtpConnectProtect == SmtpConnectProtectionType.START_TLS) {
            props.put(DEF_MAIL_TRANSPORT_PROTOCOL, "smtp");

            props.put(DEF_MAIL_SMTP_HOST, smtpConfiguration.getHost());
            props.put(DEF_MAIL_SMTP_PORT, smtpConfiguration.getPort());
            props.put(DEF_MAIL_SMTP_CONNECTION_TIMEOUT, this.connectionTimeout);
            props.put(DEF_MAIL_SMTP_TIMEOUT, this.connectionTimeout);

            props.put(DEF_MAIL_SMTP_SOCKET_FACTORY_CLASS, DEF_MAIL_SSL_SOCKET_FACTORY);
            props.put(DEF_MAIL_SMTP_SOCKET_FACTORY_PORT, smtpConfiguration.getPort());
            if (smtpConfiguration.isServerTrust()) {
                props.put(DEF_MAIL_SMTP_SSL_TRUST, smtpConfiguration.getHost());
            }
            props.put(DEF_MAIL_SMTP_STARTTLS_ENABLE, true);
            props.put(DEF_MAIL_SMTP_STARTTLS_REQUIRED, true);
        }
        else if (smtpConnectProtect == SmtpConnectProtectionType.SSL_TLS) {
            props.put(DEF_MAIL_TRANSPORT_PROTOCOL_RFC822, "smtps");

            props.put(DEF_MAIL_SMTPS_HOST, smtpConfiguration.getHost());
            props.put(DEF_MAIL_SMTPS_PORT, smtpConfiguration.getPort());
            props.put(DEF_MAIL_SMTPS_CONNECTION_TIMEOUT, this.connectionTimeout);
            props.put(DEF_MAIL_SMTPS_TIMEOUT, this.connectionTimeout);

            props.put(DEF_MAIL_SMTP_SOCKET_FACTORY_CLASS, DEF_MAIL_SSL_SOCKET_FACTORY);
            props.put(DEF_MAIL_SMTP_SOCKET_FACTORY_PORT, smtpConfiguration.getPort());
            if (smtpConfiguration.isServerTrust()) {
                props.put(DEF_MAIL_SMTP_SSL_TRUST, smtpConfiguration.getHost());
            }
            props.put(DEF_MAIL_SMTP_SSL_ENABLE, true);
        }
        else {
            props.put(DEF_MAIL_TRANSPORT_PROTOCOL, "smtp");

            props.put(DEF_MAIL_SMTP_HOST, smtpConfiguration.getHost());
            props.put(DEF_MAIL_SMTP_PORT, smtpConfiguration.getPort());
            props.put(DEF_MAIL_SMTP_CONNECTION_TIMEOUT, this.connectionTimeout);
            props.put(DEF_MAIL_SMTP_TIMEOUT, this.connectionTimeout);
        }

        Session session = null;
        if (smtpConfiguration.isRequiresAuthentication()) {

            if (smtpConnectProtect == SmtpConnectProtectionType.SSL_TLS) {
                props.put(DEF_MAIL_SMTPS_AUTH, "true");
            }
            else {
                props.put(DEF_MAIL_SMTP_AUTH, "true");
            }

            final String userName = smtpConfiguration.getSmtpAuthenticationAccountUsername();
            final String password = decrypt(smtpConfiguration.getSmtpAuthenticationAccountPassword());

            session = Session.getInstance(props, new javax.mail.Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, password);
                }
            });
        } else {
            session = Session.getInstance(props, null);
        }

        return generateSendEmail(session, smtpConfiguration, emailId, subject, body, sign);
    }

    /**
     * 
     * @param session
     * @param smtpConfiguration
     * @param emailId
     * @param subject
     * @param body
     * @param sign
     * @return
     */
    private boolean generateSendEmail(final Session session, final SmtpConfiguration smtpConfiguration,
            final String emailId, final String subject, final String body, final boolean sign) {

        PrivateKey privateKey = null;
        X509Certificate x509Certificate = null;

        if (sign) {
            try {
                privateKey = (PrivateKey)keyStore.getKey(smtpConfiguration.getKeyStoreAlias(),
                        decrypt(smtpConfiguration.getKeyStorePassword()).toCharArray());
                x509Certificate = (X509Certificate)keyStore.getCertificate(smtpConfiguration.getKeyStoreAlias());
            } catch (Exception e) {
                logger.error(e.getMessage());
            }
        }

        Message message = new MimeMessage(session);

        try {
            message.setFrom(new InternetAddress(smtpConfiguration.getFromEmailAddress()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(emailId));
            message.setSubject(subject);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(body, "text/html; charset=utf-8");

            if (sign) {
                MimeMultipart multiPart = createMultipartWithSignature(privateKey, x509Certificate, smtpConfiguration.getSigningAlgorithm(), mimeBodyPart);            
                message.setContent(multiPart);
            }
            else {
                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(mimeBodyPart);
                message.setContent(multipart);
            }

            Transport.send(message);
            return true;
        } catch (Exception e) {
            logger.error("Failed to send OTP: {}", e.getMessage());
            return false;
        }
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
    public static MimeMultipart createMultipartWithSignature(PrivateKey key, X509Certificate cert, String signingAlgorithm, MimeBodyPart dataPart) throws CertificateEncodingException, CertificateParsingException, OperatorCreationException, SMIMEException {
        List<X509Certificate> certList = new ArrayList<>();
        certList.add(cert);
        JcaCertStore certs = new JcaCertStore(certList);
        ASN1EncodableVector signedAttrs = generateSignedAttributes(cert);

        SMIMESignedGenerator gen = new SMIMESignedGenerator();

        if (Utils.isEmpty(signingAlgorithm)) {
            signingAlgorithm = cert.getSigAlgName();
        }

        gen.addSignerInfoGenerator(new JcaSimpleSignerInfoGeneratorBuilder().setProvider(SecurityProviderUtility.getBCProvider()).setSignedAttributeGenerator(new AttributeTable(signedAttrs)).build(signingAlgorithm, key, cert));

        gen.addCertificates(certs);

        return gen.generate(dataPart);
    }    

    /**
     * 
     * @param email
     * @return
     */
    public boolean isEmailRegistered(String email) {

        EmailPerson person = new EmailPerson();
        person.setMail(email);
        person.setBaseDn(persistenceService.getPeopleDn());
        logger.debug("Registered email id count: {}", persistenceService.count(person));
        return persistenceService.count(person) > 0;

    }

    /**
     * 
     * @param password
     * @return
     */
    public String encrypt(String password) {
        try {
            return Utils.stringEncrypter().encrypt(password);
        } catch (EncryptionException ex) {
            logger.error("Failed to encrypt SMTP password: ", ex);
            return null;
        }
    }

    /**
     * 
     * @param password
     * @return
     */
    public String decrypt(String password) {
        try {
            return Utils.stringEncrypter().decrypt(password);
        } catch (EncryptionException e) {
            logger.error("Unable to decrypt: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 
     * @param userId
     * @param newEmail
     * @return
     */
    public boolean addEmail(String userId, VerifiedEmail newEmail) {
        return updateEmailIdAdd(userId, getVerifiedEmail(userId), newEmail);
    }

    /**
     * 
     * @param userId
     * @param emails
     * @param newEmail
     * @return
     */
    public boolean updateEmailIdAdd(String userId, List<VerifiedEmail> emails, VerifiedEmail newEmail) {
        boolean success = false;
        try {
            EmailPerson person = persistenceService.get(EmailPerson.class, persistenceService.getPersonDn(userId));
            List<VerifiedEmail> vEmails = new ArrayList<>(emails);
            if (newEmail != null) {
                // uniqueness of the new mail has already been verified at previous step
                vEmails.add(newEmail);
            }
            List<String> mailIds = vEmails.stream().map(VerifiedEmail::getEmail).collect(Collectors.toList());
            String json = !mailIds.isEmpty() ? mapper.writeValueAsString(Collections.singletonMap("email-ids", vEmails)) : null;
            person.setJansEmail(json);
            success = persistenceService.modify(person);
            if (success && newEmail != null) {
                // modify list only if LDAP update took place
                emails.add(newEmail);
                logger.debug("Added {}", newEmail.getEmail());
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return success;
    }

    /**
     * 
     * @param nick
     * @param extraMessage
     * @return
     */
    Pair<String, String> getDeleteMessages(String nick, String extraMessage) {

        StringBuilder text = new StringBuilder();
        if (extraMessage != null) {
            text.append(extraMessage).append("\n\n");
        }
        text.append(Labels.getLabel("email_del_confirm",
                new String[] { nick == null ? Labels.getLabel("general.no_named") : nick }));
        if (extraMessage != null) {
            text.append("\n");
        }

        return new Pair<>(Labels.getLabel("email_del_title"), text.toString());

    }

    /**
     * Creates an instance of VerifiedEmail by looking up in the list of
     * VerifiedEmail passed. If the item is not found in the list, it means the user
     * had already that mail added by means of another application, ie. oxTrust. In
     * this case the resulting object will not have properties like nickname, etc.
     * Just the mail id
     * 
     * @param mail Email id (LDAP attribute "mail" inside a user entry)
     * @param list List of existing email ids enrolled. Ideally, there is an item
     *             here corresponding to the uid number passed
     * @return VerifiedMobile object
     */
    private VerifiedEmail getExtraEmailId(String mail, List<VerifiedEmail> list) {
        VerifiedEmail vEmail = new VerifiedEmail(mail);
        Optional<VerifiedEmail> extraEmail = list.stream().filter(ph -> mail.equals(ph.getEmail())).findFirst();
        if (!extraEmail.isPresent()) {
            vEmail.setNickName(mail);
            return vEmail;
        } else {
            return null;
        }
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
            if (securityMode == SecurityModeType.BCFIPS_SECURITY_MODE) {
                keyStorageType = SecurityProviderUtility.KeyStorageType.BCFKS_KS;
            }
            else if (securityMode == SecurityModeType.BCPROV_SECURITY_MODE) {
                keyStorageType = SecurityProviderUtility.KeyStorageType.PKCS12_KS;
            }
        }
        return keyStorageType;
    }
}
