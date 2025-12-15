package io.jans.casa.plugins.certauthn.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.as.common.cert.validation.*;
import io.jans.as.common.cert.validation.model.ValidationStatus;
import io.jans.as.common.cert.fingerprint.FingerprintHelper;
import io.jans.as.model.util.CertUtils;
import io.jans.casa.core.model.*;
import io.jans.casa.credential.BasicCredential;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.certauthn.model.*;
import io.jans.casa.service.IPersistenceService;
import io.jans.orm.search.filter.Filter;

import java.io.*;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

import org.slf4j.*;

import static io.jans.casa.plugins.certauthn.service.UserCertificateMatch.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class CertService {
    
    private static final String CERT_PREFIX = "cert:";
    public static String AGAMA_FLOW = "io.jans.casa.authn.cert";
    
    private Logger logger = LoggerFactory.getLogger(getClass());
	private IPersistenceService persistenceService;
    private ObjectMapper mapper;
    private boolean hasValidProperties;
    private List<X509Certificate> certChain;
    
	private static CertService SINGLE_INSTANCE = null;

    private CertService() {
        mapper = new ObjectMapper();
		persistenceService = Utils.managedBean(IPersistenceService.class);
		reloadConfiguration();
    }

	public static CertService getInstance() {
	    
		if (SINGLE_INSTANCE == null) {
            SINGLE_INSTANCE = new CertService();
		} 
		return SINGLE_INSTANCE;
		
	}
	
    public boolean isHasValidProperties() {
        return hasValidProperties;
    }
	
	public void reloadConfiguration() {
	    
        hasValidProperties = false;
	    String prop = "certChainPEM";
		String chainCerts = Optional.ofNullable(persistenceService.getAgamaFlowConfigProperties(AGAMA_FLOW))
		        .map(jo -> jo.optString(prop, null)).orElse(null);
		
		if (chainCerts == null) {
		    logger.error("Unable to read config property '{}' from flow {}", prop, AGAMA_FLOW);
		} else {
		    try (InputStream is = new ByteArrayInputStream(chainCerts.getBytes(UTF_8))) {

                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                certChain = cf.generateCertificates(is).stream().map(X509Certificate.class::cast)
                        .collect(Collectors.toList());

                logger.info("{} certs loaded from certificate chain", certChain.size());
                hasValidProperties = true;
		    } catch (Exception e) {
                logger.error(e.getMessage(), e);
		    }
		}
		
	}

    public List<Certificate> getUserCerts(String userId) {

        List<Certificate> certs = new ArrayList<>();
        try {
            CertPerson person = persistenceService.get(CertPerson.class, persistenceService.getPersonDn(userId));

            List<io.jans.scim.model.scim2.user.X509Certificate> x509Certificates = getScimX509Certificates(
                    Optional.ofNullable(person.getX509Certificates()).orElse(Collections.emptyList()));

            certs = person.getJansExtUid().stream().filter(uid -> uid.startsWith(CERT_PREFIX))
                    .map(uid -> getExtraCertsInfo(uid, x509Certificates)).collect(Collectors.toList());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return certs;

    }

    public int getDevicesTotal(String userId) {

        int total = 0;
        try {
            IdentityPerson person = persistenceService.get(IdentityPerson.class, persistenceService.getPersonDn(userId));
            total = (int) person.getJansExtUid().stream().filter(uid -> uid.startsWith(CERT_PREFIX)).count();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return total;

    }
    
    public UserCertificateMatch processMatch(X509Certificate certificate, String userInum, boolean enroll) {

        UserCertificateMatch status = null;
        try {
            logger.info("Matching certificate and user. Enrollment is {}", enroll);
            CertPerson person = persistenceService.get(CertPerson.class, persistenceService.getPersonDn(userInum));

            if (person == null) {
                status = UNKNOWN_USER;
            } else {
                logger.debug("Generating certificate fingerprint...");
                String fingerprint = getFingerPrint(certificate);
                String externalUid = String.format("%s%s", CERT_PREFIX, fingerprint);

                Filter filter = Filter.createEqualityFilter("jansExtUid", externalUid).multiValued();
                List<BasePerson> people = persistenceService.find(BasePerson.class, persistenceService.getPeopleDn(), filter, 0, 1);

                //The list should be singleton at most
                if (people.size() > 0) {
                    if (userInum.equals(people.get(0).getInum())) {
                        status = enroll ? CERT_ENROLLED_ALREADY : SUCCESS;
                    } else {
                        status = CERT_ENROLLED_OTHER_USER;
                    }
                } else {
                    if (enroll) {
                        logger.info("Associating presented cert to user");
                        List<String> oeuid = new ArrayList<>(Optional.ofNullable(person.getJansExtUid()).orElse(Collections.emptyList()));
                        oeuid.add(externalUid);
                        person.setJansExtUid(oeuid);

                        status = SUCCESS;
                    } else {
                        logger.info("Certificate not associated to an existing account yet");
                        status = CERT_NOT_RECOGNIZED;
                    }
                }
            }
            if (status.equals(SUCCESS) || status.equals(CERT_ENROLLED_ALREADY)) {
                updateUserX509Certificates(person, certificate);
                status = persistenceService.modify(person) ? status : UNKNOWN_ERROR;
            }
            logger.info("Operation result is {}", status.toString());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            status = UNKNOWN_ERROR;
        }
        return status;

    }

    public boolean removeFromUser(String fingerPrint, String userId) throws Exception {

        CertPerson person = persistenceService.get(CertPerson.class, persistenceService.getPersonDn(userId));

        List<String> stringCerts = Optional.ofNullable(person.getX509Certificates()).orElse(new ArrayList<>());
        List<io.jans.scim.model.scim2.user.X509Certificate> scimCerts = getScimX509Certificates(stringCerts);

        boolean found = false;
        int i;
        for (i = 0; i < scimCerts.size() && !found; i++) {
            found = getFingerPrint(CertUtils.x509CertificateFromPem(scimCerts.get(i).getValue())).equals(fingerPrint);
        }
        if (found) {
            person.getX509Certificates().remove(i - 1);
        }

        Optional<String> externalUid = person.getJansExtUid().stream()
                .filter(str -> str.equals(CERT_PREFIX + fingerPrint)).findFirst();
        externalUid.ifPresent(uid ->  person.getJansExtUid().remove(uid));

        return persistenceService.modify(person);

    }

    public boolean validate(X509Certificate cert) {
        
        logger.info("Validating certificate");
        PathCertificateVerifier verifier = new PathCertificateVerifier(true);

        ValidationStatus.CertificateValidity validity = verifier.validate(cert, certChain, new Date()).getValidity();
        boolean valid = validity.equals(ValidationStatus.CertificateValidity.VALID);
        logger.warn("Certificate is {}valid", valid ? "": "not ");
        
        return valid;

    }

    private void updateUserX509Certificates(CertPerson person, X509Certificate certificate) {

        try {
            boolean match = false;
            String display = certificate.getSubjectX500Principal().getName();

            logger.info("Reading user's stored X509 certificates");
            List<String> stringCerts = Optional.ofNullable(person.getX509Certificates()).orElse(new ArrayList<>());
            List<io.jans.scim.model.scim2.user.X509Certificate> scimCerts = getScimX509Certificates(stringCerts);

            for (io.jans.scim.model.scim2.user.X509Certificate scimCert : scimCerts) {
                String scimDisplay = scimCert.getDisplay();
                if (Utils.isNotEmpty(scimDisplay) && scimDisplay.equals(display)) {
                    logger.debug("The certificate presented is already in user's profile");
                    match = true;
                    break;
                }
            }

            if (!match) {
                io.jans.scim.model.scim2.user.X509Certificate scimX509Cert = new io.jans.scim.model.scim2.user.X509Certificate();
                byte DEREncoded[] = certificate.getEncoded();
                scimX509Cert.setValue(new String(Base64.getEncoder().encode(DEREncoded), UTF_8));
                scimX509Cert.setDisplay(display);

                logger.debug("Updating user's oxTrustx509Certificate attribute");
                stringCerts.add(mapper.writeValueAsString(scimX509Cert));
                person.setX509Certificates(stringCerts);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

    private Certificate getExtraCertsInfo(String externalUid, List<io.jans.scim.model.scim2.user.X509Certificate> scimCerts) {

        String fingerPrint = externalUid.replace(CERT_PREFIX, "");
        Certificate cert = new Certificate();
        cert.setFingerPrint(fingerPrint);

        for (io.jans.scim.model.scim2.user.X509Certificate sc : scimCerts) {
            try {
                X509Certificate x509Certificate = CertUtils.x509CertificateFromPem(sc.getValue());
                if (fingerPrint.equals(getFingerPrint(x509Certificate))) {

                    Map<String, String> attributes = Arrays.stream(sc.getDisplay().split(",\\s*"))
                            .collect(Collectors.toMap(t -> t.substring(0,t.indexOf('=')).toLowerCase(), t -> t.substring(t.indexOf('=') + 1)));

                    String cn = attributes.get("cn");
                    String ou = attributes.getOrDefault("ou", "");
                    String o = attributes.getOrDefault("o", "");

                    if (Utils.isNotEmpty(ou)) {
                        if (Utils.isNotEmpty(o)) {
                            ou += ", " + o;
                        }
                    } else if (Utils.isNotEmpty(o)){
                        ou = o;
                    }

                    String l = attributes.getOrDefault("l", "");
                    String st = attributes.getOrDefault("st", "");
                    String c = attributes.getOrDefault("c", "");

                    cert.setCommonName(cn);
                    cert.setOrganization(ou);
                    cert.setLocation(String.format("%s %s %s", l, st, c).trim());

                    cert.setFormattedName(cn + (Utils.isEmpty(ou) ? "" : String.format(" (%s)", ou)));

                    long date = x509Certificate.getNotAfter().getTime();
                    cert.setExpirationDate(date);
                    cert.setExpired(date < System.currentTimeMillis());

                    break;
                }
            } catch (Exception e) {
                logger.error(e.getMessage());
            }

        }
        return cert;

    }

    private List<io.jans.scim.model.scim2.user.X509Certificate> getScimX509Certificates(List<String> scimStringCerts) {

        List<io.jans.scim.model.scim2.user.X509Certificate> scimCerts = new ArrayList<>();
        for (String scimCert : scimStringCerts) {
            try {
                scimCerts.add(mapper.readValue(scimCert, io.jans.scim.model.scim2.user.X509Certificate.class));
            } catch (Exception e) {
                logger.error("Unable to convert value '{}' to expected SCIM format", scimCert);
                logger.error(e.getMessage());
            }
        }
        return scimCerts;

    }
    
    private String getFingerPrint(X509Certificate certificate) throws Exception {
        return FingerprintHelper.getPublicKeySshFingerprint(certificate.getPublicKey());
    }

}
