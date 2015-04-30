package org.xdi.oxd.licenser.server.service;

import com.google.common.base.Strings;
import com.google.common.io.BaseEncoding;
import com.google.inject.Inject;
import com.unboundid.ldap.sdk.Filter;
import net.nicholaswilliams.java.licensing.encryption.RSAKeyPairGenerator;
import org.apache.commons.lang.RandomStringUtils;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;
import org.xdi.oxd.license.client.lib.LicenseSerializationUtilities;
import org.xdi.oxd.license.client.js.Configuration;
import org.xdi.oxd.licenser.server.ldap.LdapStructure;

import java.security.KeyPair;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/10/2014
 */

public class LicenseCryptService {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseCryptService.class);

    @Inject
    LdapEntryManager ldapEntryManager;
    @Inject
    Configuration conf;
    @Inject
    LdapStructure ldapStructure;

    public LdapLicenseCrypt generate() {
        RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
        KeyPair keyPair = generator.generateKeyPair();

        final String privatePassword = randomPassword();
        final String publicPassword = randomPassword();
        final byte[] privateKeyBytes = LicenseSerializationUtilities.writeEncryptedPrivateKey(keyPair.getPrivate(), privatePassword.toCharArray());
        final byte[] publicKeyBytes = LicenseSerializationUtilities.writeEncryptedPublicKey(keyPair.getPublic(), publicPassword.toCharArray());
        return new LdapLicenseCrypt().
                setPrivateKey(BaseEncoding.base64().encode(privateKeyBytes)).
                setPublicKey(BaseEncoding.base64().encode(publicKeyBytes)).
                setPrivatePassword(privatePassword).
                setPublicPassword(publicPassword).
                setLicensePassword(randomPassword());
    }

    public String randomPassword() {
        return RandomStringUtils.randomAlphanumeric(20);
    }

    public List<LdapLicenseCrypt> getAll() {
        try {
            final Filter filter = Filter.create("&(uniqueIdentifier=*)");
            return ldapEntryManager.findEntries(ldapStructure.getLicenseCryptBaseDn(), LdapLicenseCrypt.class, filter);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public LdapLicenseCrypt get(String dn) {
        return ldapEntryManager.find(LdapLicenseCrypt.class, dn);
    }


    public void merge(LdapLicenseCrypt entity) {
        try {
            ldapEntryManager.merge(entity);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void save(LdapLicenseCrypt entity) {
        try {
            setDnIfEmpty(entity);
            ldapEntryManager.persist(entity);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void setDnIfEmpty(LdapLicenseCrypt entity) {
        if (Strings.isNullOrEmpty(entity.getDn())) {
            String id = Strings.isNullOrEmpty(entity.getId()) ? UUID.randomUUID().toString() : entity.getId();
            entity.setId(id);
            entity.setDn(String.format("uniqueIdentifier=%s,%s", id, ldapStructure.getLicenseCryptBaseDn()));
        }
    }

    public void remove(LdapLicenseCrypt entity) {
        try {
            ldapEntryManager.remove(entity);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }
}
