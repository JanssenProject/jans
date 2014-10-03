package org.xdi.oxd.licenser.server.service;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.unboundid.ldap.sdk.Filter;
import org.gluu.site.ldap.persistence.LdapEntryManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.licenser.server.conf.Configuration;
import org.xdi.oxd.licenser.server.ldap.LdapCustomer;
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
            final Filter filter = Filter.create("&(customerId=*)");
            return ldapEntryManager.findEntries(ldapStructure.getCustomerBaseDn(), LdapCustomer.class, filter);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return Collections.emptyList();
    }

    public LdapCustomer get(String dn) {
        return ldapEntryManager.find(LdapCustomer.class, dn);
    }

    public LdapCustomer getCustomersByLicenseId(String licenseId) {
        try {
//                   final Filter filter = Filter.create("&(inum=*)");
            final Filter filter = Filter.create(String.format("&(licenseId=%s)", licenseId));
            final List<LdapCustomer> entries = ldapEntryManager.findEntries(ldapStructure.getCustomerBaseDn(), LdapCustomer.class, filter);
            if (!entries.isEmpty()) {
                return entries.get(0);
            } else {
                LOG.error("There no customer object with licenseId:" + licenseId);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
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
            entity.setDn(String.format("customerId=%s,%s", id, ldapStructure.getCustomerBaseDn()));
        }
    }

}
