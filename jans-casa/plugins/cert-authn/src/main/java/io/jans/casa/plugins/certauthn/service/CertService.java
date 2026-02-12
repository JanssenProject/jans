package io.jans.casa.plugins.certauthn.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.unboundid.ldap.sdk.*;

import io.jans.as.model.util.CertUtils;
import io.jans.casa.core.model.*;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.certauthn.model.*;
import io.jans.casa.service.IPersistenceService;
import io.jans.orm.search.filter.Filter;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.*;

import static java.nio.charset.StandardCharsets.UTF_8;

public class CertService {

    public static final String AGAMA_FLOW = "io.jans.casa.authn.cert";
    public static final String AGAMA_ENROLLMENT_FLOW = "io.jans.casa.cert.enroll";

    private static final String CERT_PREFIX = "cert:";

    private Logger logger = LoggerFactory.getLogger(getClass());
	private IPersistenceService persistenceService;
    private ObjectMapper mapper;
    
	private static CertService SINGLE_INSTANCE = null;

    private CertService() {
        mapper = new ObjectMapper();
		persistenceService = Utils.managedBean(IPersistenceService.class);
    }

	public static CertService getInstance() {
	    
		if (SINGLE_INSTANCE == null) {
            SINGLE_INSTANCE = new CertService();
		} 
		return SINGLE_INSTANCE;
		
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

    public boolean removeFromUser(Certificate certificate, String userId) throws Exception {

        CertPerson person = persistenceService.get(CertPerson.class, persistenceService.getPersonDn(userId));
        List<String> stringCerts = Optional.ofNullable(person.getX509Certificates()).orElse(new ArrayList<>());
        List<io.jans.scim.model.scim2.user.X509Certificate> scimCerts = getScimX509Certificates(stringCerts);

        boolean found = false;
        int i;
        for (i = 0; i < scimCerts.size() && !found; i++) {
            String val = scimCerts.get(i).getValue();
            found = val != null && val.equals(certificate.getPemContent());
        }
        if (found) {
            logger.info("Removing cert from SCIM profile data"); 
            person.getX509Certificates().remove(i - 1);
        }
        person.getJansExtUid().remove(CERT_PREFIX + certificate.getFingerPrint());

        logger.info("Removing cert reference from user");
        return persistenceService.modify(person);

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
                    cert.setPemContent(sc.getValue());

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
                        String name = attr.getName().toLowerCase();
                        if (!map.containsKey(name)) {
                            map.put(name, attr.getValues()[0]);
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
