package io.jans.casa.plugins.certauthn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unboundid.ldap.sdk.*;

import io.jans.as.model.util.CertUtils;
import io.jans.casa.core.model.*;
import io.jans.casa.credential.BasicCredential;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.certauthn.model.*;
import io.jans.casa.service.IPersistenceService;
import io.jans.orm.search.filter.Filter;

import java.io.*;
import java.security.cert.CertificateException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.function.Function;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;
import org.slf4j.*;

import static io.jans.casa.plugins.certauthn.service.UserCertificateMatch.*;
import static java.nio.charset.StandardCharsets.UTF_8;

public class CertService {
    
    private static final String CERT_PREFIX = "cert:";
    public static final String AGAMA_FLOW = "io.jans.casa.authn.cert";
    
    private Logger logger = LoggerFactory.getLogger(getClass());
	private IPersistenceService persistenceService;
	private String certPickupUrl;
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
		JSONObject props = Optional.ofNullable(
		            persistenceService.getAgamaFlowConfigProperties(AGAMA_FLOW)).orElse(new JSONObject());
			
		String pemString = props.optString("certChainPEM", null);
		certPickupUrl = props.optString("certPickupUrl", null);
		
		if (pemString == null || certPickupUrl == null ) {
		    logger.error("Unable to read required config properties from flow {}", AGAMA_FLOW);
		} else {
		    try (InputStream is = new ByteArrayInputStream(pemString.getBytes(UTF_8))) {

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
	
	public String getCertPickupUrl() {
	    return certPickupUrl;
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
                String externalUid = String.format("%s%s", CERT_PREFIX, getFingerPrint(certificate));

                Filter filter = Filter.createEqualityFilter("jansExtUid", externalUid).multiValued();
                List<BasePerson> people = persistenceService.find(
                        BasePerson.class, persistenceService.getPeopleDn(), filter, 0, 1);

                //The list should be singleton at most
                if (people.size() > 0) {
                    if (userInum.equals(people.get(0).getInum())) {
                        status = enroll ? CERT_ENROLLED_ALREADY : SUCCESS;
                    } else {
                        status = CERT_ENROLLED_OTHER_USER;
                    }
                } else if (enroll) {
                    logger.info("Associating presented cert to user");
                    List<String> oeuid = new ArrayList<>(
                            Optional.ofNullable(person.getJansExtUid()).orElse(Collections.emptyList()));

                    oeuid.add(externalUid);                    
                    person.setJansExtUid(oeuid);
                    updateUserX509Certificates(person, certificate);
                    
                    status = persistenceService.modify(person) ? SUCCESS : UNKNOWN_ERROR;
                } else {
                    logger.info("Certificate not associated to an existing account yet");
                    status = CERT_NOT_RECOGNIZED;
                }
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
            String val = scimCerts.get(i).getValue();
            found = getFingerPrint(CertUtils.x509CertificateFromPem(val)).equals(fingerPrint);
        }
        if (found) {
            logger.info("Removing cert from SCIM profile data"); 
            person.getX509Certificates().remove(i - 1);
        }
        person.getJansExtUid().remove(CERT_PREFIX + fingerPrint);

        logger.info("Removing cert reference from user");
        return persistenceService.modify(person);

    }

    public boolean validate(X509Certificate cert) {

        logger.info("Validating certificate...");
        PathCertificateVerifier verifier = new PathCertificateVerifier(true);
        return verifier.validate(cert, certChain);

    }

    private void updateUserX509Certificates(CertPerson person, X509Certificate certificate) {

        List<String> stringCerts = Optional.ofNullable(person.getX509Certificates()).orElse(new ArrayList<>());
        io.jans.scim.model.scim2.user.X509Certificate scimX509Cert = new io.jans.scim.model.scim2.user.X509Certificate();
        try {
            byte[] DEREncoded = certificate.getEncoded();
            scimX509Cert.setValue(new String(Base64.getEncoder().encode(DEREncoded), UTF_8));
            scimX509Cert.setDisplay(certificate.getSubjectX500Principal().getName());

            logger.debug("Updating user's jans509Certificate attribute");
            stringCerts.add(mapper.writeValueAsString(scimX509Cert));
            person.setX509Certificates(stringCerts);

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

                    //Break the subject DN into its several pieces and store them in a map
                    Map<String, String> attributes = getDNAttributes(sc.getDisplay());

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
    
    private String getFingerPrint(X509Certificate certificate) throws CertificateEncodingException {
        return DigestUtils.sha1Hex(certificate.getEncoded());
    }

    private Map<String, String> getDNAttributes(String dn) {
        
        Map<String, String> map = new HashMap<String, String>();
        
        if (Utils.isNotEmpty(dn)) {
            try {
                RDN[] rdns = DN.getRDNs(dn);
                
                //Collect the different attribute names and values in the map. The left-most attributes
                //found are preferred over others when there are several attributes with the same name
                for (RDN rdn: rdns) {
                    Attribute[] attrs = rdn.getAttributes();

                    for (Attribute attr : attrs) {
                        if (!map.containsKey(attr.getName())) {
                            map.put(attr.getName(), attr.getValues()[0]);
                        }
                    }
                }
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
        return map;
        
    }
    
}
