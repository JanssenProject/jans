package org.xdi.oxd.licenser.server.service;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.unboundid.ldap.sdk.Filter;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.licenser.server.conf.Configuration;
import org.xdi.oxd.licenser.server.ldap.LdapStructure;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 05/10/2014
 */

public class LicenseIdService {

    private static final Logger LOG = LoggerFactory.getLogger(LicenseIdService.class);

    @Inject
    LdapEntryManager ldapEntryManager;
    @Inject
    Configuration conf;
    @Inject
    LdapStructure ldapStructure;

    public List<LdapLicenseId> getAll() {
        try {
            final Filter filter = Filter.create("&(licenseId=*)");
            return ldapEntryManager.findEntries(ldapStructure.getLicenseIdBaseDn(), LdapLicenseId.class, filter);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public List<LdapLicenseId> getByCryptDn(String cryptDn) {
        try {
            final Filter filter = Filter.create(String.format("&(oxLicenseCrypt=%s)", cryptDn));
            return ldapEntryManager.findEntries(ldapStructure.getLicenseIdBaseDn(), LdapLicenseId.class, filter);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public LdapLicenseId get(String dn) {
        return ldapEntryManager.find(LdapLicenseId.class, dn);
    }


    public void merge(LdapLicenseId entity) {
        try {
            ldapEntryManager.merge(entity);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void save(LdapLicenseId entity) {
        try {
            setDnIfEmpty(entity);
            ldapEntryManager.persist(entity);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void setDnIfEmpty(LdapLicenseId entity) {
        if (Strings.isNullOrEmpty(entity.getDn())) {
            String id = Strings.isNullOrEmpty(entity.getLicenseId()) ? generateLicenseId() : entity.getLicenseId();
            entity.setLicenseId(id);
            entity.setDn(String.format("licenseId=%s,%s", id, ldapStructure.getLicenseIdBaseDn()));
        }
    }

    public void remove(LdapLicenseId entity) {
        try {
            ldapEntryManager.remove(entity);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private String generateLicenseId() {
        return UUID.randomUUID().toString();
    }

    public LdapLicenseId generate() {
        LdapLicenseId id = new LdapLicenseId();
        setDnIfEmpty(id);
        return id;
    }
}
