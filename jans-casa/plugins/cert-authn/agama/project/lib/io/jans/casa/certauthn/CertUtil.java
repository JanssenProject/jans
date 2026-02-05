package io.jans.casa.certauthn;

import com.unboundid.ldap.sdk.*;

import io.jans.as.common.cert.validation.PathCertificateVerifier;
import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.UserService;
import io.jans.as.model.util.CertUtils;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.util.Pair;

import java.lang.reflect.Field;
import java.io.*;
import java.security.cert.X509Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONObject;
import org.slf4j.*;

import static io.jans.as.common.cert.validation.model.ValidationStatus.CertificateValidity.VALID;
import static java.nio.charset.StandardCharsets.UTF_8;

public final class CertUtil {
    
    private static final String EXT_UID = "jansExtUid";
    private static final String CERT_PREFIX = "cert:";
    
    private static UserService userService = CdiUtil.bean(UserService.class);
    private static Logger logger = LoggerFactory.getLogger(CertAuthnHelper.class);
    
    public static Pair<ValidationStatus, X509Certificate> validate(String certPEM, String certChainPEM) {
        
        Pair<ValidationStatus, X509Certificate> p = new Pair<>();
        
        if (certPEM == null) {
            logger.warn("No client certificate was received. Probably the user hit the Cancel button in the browser prompt");
            p.setFirst(ValidationStatus.NOT_SELECTED);
            return p;
        }
        
        logger.debug("Parsing certificate...");
        X509Certificate certificate = CertUtils.x509CertificateFromPem(certPEM);
        if (certificate == null) {
            p.setFirst(ValidationStatus.UNPARSABLE);
            return p;
        }
        
        List<X509Certificate> certChain;
        try (InputStream is = new ByteArrayInputStream(certChainPEM.getBytes(UTF_8))) {

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            certChain = cf.generateCertificates(is).stream().map(X509Certificate.class::cast)
                    .collect(Collectors.toList());
            
        } catch (CertificateException e) {
            logger.error(e.getMessage(), e);
            p.setFirst(ValidationStatus.UNPARSABLE);
            return p;
        }

        logger.debug("{} certs loaded from certificate chain", certChain.size());
        
        PathCertificateVerifier verifier = new PathCertificateVerifier(false);       
        boolean valid = verifier.validate(certificate, certChain, new Date()).getValidity().equals(VALID);
        
        if (valid) {
            p.setFirst(ValidationStatus.VALID);
            p.setSecond(certificate);
        } else {
            p.setFirst(ValidationStatus.NOT_VALID);
        }

        return p;
        
    }
    
    public static EnrollmentStatus enroll(String uid, X509Certificate certificate, String fingerPrint) {

        User user = userService.getUser(uid);
        if (user == null) {
            return EnrollmentStatus.UNKNOWN_USER;
        }

        try {
            String ownerUid = findOwner(fingerPrint);
            if (ownerUid == null) {
                
                logger.debug("Computing new jansExtUid attribute");
                //update user's jansExtUid attribute
                String[] extUidsArr = Optional.ofNullable(user.getExternalUid()).orElse(new String[0]);
                List<String> extUids = new ArrayList<>(Arrays.asList(extUidsArr));
                
                extUids.add(CERT_PREFIX + fingerPrint);
                user.setExternalUid(extUids.toArray(new String[0]));
                
                logger.debug("Computing new jans509Certificate attribute");
                String jans509Cert = makeJans509(certificate);
                List<CustomObjectAttribute> customAttrs = new ArrayList<>(user.getCustomAttributes());
                
                CustomObjectAttribute jans509COA = customAttrs.stream()
                        .filter(ca -> ca.getName().equals("jans509Certificate")).findFirst().orElse(null);
    
                if (jans509COA != null) {
                    List<Object> values = new ArrayList<>(jans509COA.getValues());
                    values.add(jans509Cert);
                    jans509COA.setValues(values);                
                } else {
                    jans509COA = new CustomObjectAttribute("jans509Certificate", jans509Cert);
                    customAttrs.add(jans509COA);
                    user.setCustomAttributes(customAttrs);
                }
                
                logger.debug("Updating user...");
                userService.updateUser(user);
                return EnrollmentStatus.SUCCESS;
    
            } else if (ownerUid.equals(uid)) {
                //Nothing to do, certificate already associated to the account
                return EnrollmentStatus.CERT_ENROLLED_ALREADY;
            } else {
                //This cert is owned by another user
                return EnrollmentStatus.CERT_ENROLLED_OTHER_USER;
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return EnrollmentStatus.ERROR;
        }
        
    }
    
    public static String findOwner(String fingerPrint) {
        
        User user = userService.getUserByAttribute(EXT_UID, CERT_PREFIX + fingerPrint, true);
        String uid = user == null ? null : user.getUserId();
        logger.info("User associated to the given certificate fingerprint was {}found", uid == null ? "not ": "");
        return uid;
        
    }
    
    public static String register(X509Certificate certificate, String classField)
        throws IllegalAccessException {
        
        Class<?> clazz = AttributeMappings.class;
        Field f = clazz.getDeclaredField(classField);
        logger.debug("Retrieving value of mapping field {}", f.getName());
        UnaryOperator<Map<String, String>> op = (UnaryOperator<Map<String, String>>) f.get(clazz);
        
        Map<String, String> certAttributes = getAttributes(certificate);
        logger.info("Applying mapping to certificate attributes...");
        Map<String, String> profile = op.apply(certAttributes);
        
        logger.info("Resulting profile: {}", profile);
        
        String uid = profile.get("uid");
        if (uid == null) {
            logger.error("Profile is missing UID attribute!");
            return null;
        }

        if (userService.getUser(uid) != null) {
            logger.debug("User already exists. Profile left untouched");
            return uid;
        }
            
        List<CustomObjectAttribute> attrs = new ArrayList<>();        
        profile.forEach((k, v) -> {
                if (v != null) {
                    CustomObjectAttribute coa = new CustomObjectAttribute(k);
                    coa.setValue(v);
                    attrs.add(coa);
                }
        });
        
        logger.info("Onboarding a new user...");
        uid = null;   
        User user = new User();
        try {
            user.setCustomAttributes(attrs);
            //Persist new user
            user = userService.addUser(user, true);
            //Ensure it was effectively saved
            uid = user.getUserId();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return uid;
        
    }
        
    public static String getFingerPrint(X509Certificate certificate) throws CertificateEncodingException {
        return DigestUtils.sha1Hex(certificate.getEncoded());
    }

    private static Map<String, String> getAttributes(X509Certificate cert) {
        
        String dn = cert.getSubjectX500Principal().getName()
        Map<String, String> map = new HashMap<String, String>();
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
            
            //Process the Subject Alternative Names, if any, to find an e-mail 
            Iterator<List<?>> it = Optional.ofNullable(cert.getSubjectAlternativeNames())
                    .map(Collection::iterator).orElse(null);
                    
            if (it != null) {
                Integer rfc822NameType = 1;
                String email = null;

                while (it.hasNext() && email == null) {
                    List<?> list = it.next();
                    
                    if (list != null && list.size() == 2 && rfc822NameType.equals(list.get(0))
                            && list.get(1).toString().contains("@")) {
                        email = list.get(1).toString();
                        logger.info("Found e-mail in SAN extension: {}", email);
                    }
                }
                map.put("mail", email);                
            }
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return map;
        
    }
    
    private static List<CustomObjectAttribute> attributesForUpdate(List<CustomObjectAttribute> customAttributes,
            Map<String, List<Object>> profile, boolean cumulative) {

        //Merge existing data of user plus incoming data in profile
        List<CustomObjectAttribute> customAttrs = new ArrayList<>(customAttributes);

        for (CustomObjectAttribute coa : customAttrs) {
            String attrName = coa.getName();
            List<Object> newValues = profile.get(attrName);

            if (newValues != null) {
                List<Object> values = new ArrayList<>(cumulative ? coa.getValues() : Collections.emptyList());
                newValues.stream().filter(nv -> !values.contains(nv)).forEach(values::add);

                profile.remove(attrName);
                coa.setValues(values);
            }
        }

        profile.forEach((k, v) -> {
                CustomObjectAttribute coa = new CustomObjectAttribute(k);
                coa.setValues(v);
                customAttrs.add(coa);
        });

        logger.trace("Resulting list of attributes:\n{}", customAttrs.toString());
        return customAttrs;

    }
    
    private static String makeJans509(X509Certificate certificate) {

        byte[] DEREncoded = certificate.getEncoded();
        io.jans.scim.model.scim2.user.X509Certificate scimX509Cert = new io.jans.scim.model.scim2.user.X509Certificate();
        
        scimX509Cert.setValue(new String(Base64.getEncoder().encode(DEREncoded), UTF_8));
        scimX509Cert.setDisplay(certificate.getSubjectX500Principal().getName());

        return new JSONObject(scimX509Cert).toString();

    }
    
    private CertUtil() { }
    
}
