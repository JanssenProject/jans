package org.xdi.oxd.license.admin.server;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.nicholaswilliams.java.licensing.encryption.RSAKeyPairGenerator;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.license.admin.client.service.AdminService;
import org.xdi.oxd.license.admin.shared.Customer;
import org.xdi.oxd.license.admin.shared.GeneratedKeys;
import org.xdi.oxd.license.admin.shared.License;
import org.xdi.oxd.licenser.server.ldap.LdapCustomer;
import org.xdi.oxd.licenser.server.persistence.CustomerService;

import java.security.KeyPair;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

@Singleton
public class AdminServiceImpl extends RemoteServiceServlet implements AdminService {

    private static final Logger LOG = LoggerFactory.getLogger(AdminServiceImpl.class);

    @Inject
    CustomerService customerService;

    @Override
    public List<Customer> getCustomers() {
        List<Customer> result = Lists.newArrayList();
        try {
            for (LdapCustomer ldapCustomer : customerService.getAllCustomers()) {
                result.add(asCustomer(ldapCustomer));
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return result;
    }

    @Override
    public void save(Customer customer) {
        customerService.save(asLdapCustomer(customer));
    }

    @Override
    public void create(Customer customer) {
        customerService.save(asLdapCustomer(customer));
    }

    @Override
    public GeneratedKeys generateKeys() {
        RSAKeyPairGenerator generator = new RSAKeyPairGenerator();
        KeyPair keyPair = generator.generateKeyPair();

        return new GeneratedKeys().
                setPrivateKey(BaseEncoding.base64().encode(keyPair.getPrivate().getEncoded())).
                setPublicKey(BaseEncoding.base64().encode(keyPair.getPublic().getEncoded())).
                setPrivatePassword(randomPassword()).
                setPublicPassword(randomPassword()).
                setLicensePassword(randomPassword());
    }

    private String randomPassword() {
        return RandomStringUtils.randomAlphanumeric(20);
    }

    private static LdapCustomer asLdapCustomer(Customer c) {
        LdapCustomer customer = new LdapCustomer();
        customer.setDn(c.getDn());
        customer.setId(c.getId());
        customer.setName(c.getName());
        customer.setLicensePassword(c.getLicensePassword());
        customer.setPrivatePassword(c.getPrivatePassword());
        customer.setPublicPassword(c.getPublicPassword());
        customer.setPrivateKey(c.getPrivateKey());
        customer.setPublicKey(c.getPublicKey());
        customer.setClientPrivateKey(c.getClientPrivateKey());
        customer.setClientPublicKey(c.getClientPublicKey());
        customer.setLicenses(toLicenseList(c.getLicenses()));
        return customer;
    }

    private static List<String> toLicenseList(List<License> licenses) {
        List<String> result = Lists.newArrayList(); // todo
        return result;
    }

    private static List<License> toLicenses(List<String> licenses) {
        List<License> result = Lists.newArrayList();    // todo
        return result;
    }

    private static Customer asCustomer(LdapCustomer c) {
        Customer customer = new Customer();
        customer.setDn(c.getDn());
        customer.setId(c.getId());
        customer.setName(c.getName());
        customer.setLicensePassword(c.getLicensePassword());
        customer.setPrivatePassword(c.getPrivatePassword());
        customer.setPublicPassword(c.getPublicPassword());
        customer.setPrivateKey(c.getPrivateKey());
        customer.setPublicKey(c.getPublicKey());
        customer.setClientPrivateKey(c.getClientPrivateKey());
        customer.setClientPublicKey(c.getClientPublicKey());
        customer.setLicenses(toLicenses(c.getLicenses()));
        return customer;
    }


}
