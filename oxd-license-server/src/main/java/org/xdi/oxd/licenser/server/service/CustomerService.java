package org.xdi.oxd.licenser.server.service;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.unboundid.ldap.sdk.Filter;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.licenser.server.conf.Configuration;
import org.xdi.oxd.license.client.js.LdapCustomer;
import org.xdi.oxd.licenser.server.ldap.LdapStructure;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 14/09/2014
 */

public class CustomerService {

    private static final Logger LOG = LoggerFactory.getLogger(CustomerService.class);

    @Inject
    LdapEntryManager ldapEntryManager;
    @Inject
    Configuration conf;
    @Inject
    LdapStructure ldapStructure;

    public List<LdapCustomer> getAll() {
        try {
            final Filter filter = Filter.create("&(uniqueIdentifier=*)");
            return ldapEntryManager.findEntries(ldapStructure.getCustomerBaseDn(), LdapCustomer.class, filter);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public LdapCustomer get(String dn) {
        try {
            return ldapEntryManager.find(LdapCustomer.class, dn);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public void remove(LdapCustomer entity) {
        try {
            ldapEntryManager.remove(entity);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void merge(LdapCustomer entity) {
        try {
            ldapEntryManager.merge(entity);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public void save(LdapCustomer entity) {
        try {
            setDnIfEmpty(entity);
            ldapEntryManager.persist(entity);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void setDnIfEmpty(LdapCustomer entity) {
        if (Strings.isNullOrEmpty(entity.getDn())) {
            String id = Strings.isNullOrEmpty(entity.getId()) ? UUID.randomUUID().toString() : entity.getId();
            entity.setId(id);
            entity.setDn(String.format("uniqueIdentifier=%s,%s", id, ldapStructure.getCustomerBaseDn()));
        }
    }

}
