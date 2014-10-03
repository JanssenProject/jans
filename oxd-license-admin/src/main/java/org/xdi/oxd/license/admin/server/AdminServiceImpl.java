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
import org.xdi.oxd.license.admin.shared.CustomerLicenseId;
import org.xdi.oxd.license.admin.shared.GeneratedKeys;
import org.xdi.oxd.license.admin.shared.LicenseMetadata;
import org.xdi.oxd.license.client.data.License;
import org.xdi.oxd.licenser.server.LicenseGenerator;
import org.xdi.oxd.licenser.server.LicenseGeneratorInput;
import org.xdi.oxd.licenser.server.LicenseSerializationUtilities;
import org.xdi.oxd.licenser.server.ldap.LdapCustomer;
import org.xdi.oxd.licenser.server.service.CustomerService;
import org.xdi.oxd.licenser.server.service.LicenseCryptService;

import java.security.KeyPair;
import java.util.Date;
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
    @Inject
    LicenseCryptService licenseCryptService;

    @Override
    public List<Customer> getCustomers() {
        List<Customer> result = Lists.newArrayList();
        try {
            for (LdapCustomer ldapCustomer : customerService.getAll()) {
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

        final String privatePassword = randomPassword();
        final String publicPassword = randomPassword();
        final byte[] privateKeyBytes = LicenseSerializationUtilities.writeEncryptedPrivateKey(keyPair.getPrivate(), privatePassword.toCharArray());
        final byte[] publicKeyBytes = LicenseSerializationUtilities.writeEncryptedPublicKey(keyPair.getPublic(), publicPassword.toCharArray());
        return new GeneratedKeys().
                setPrivateKey(BaseEncoding.base64().encode(privateKeyBytes)).
                setPublicKey(BaseEncoding.base64().encode(publicKeyBytes)).
                setPrivatePassword(privatePassword).
                setPublicPassword(publicPassword).
                setLicensePassword(randomPassword());
    }

    @Override
    public LicenseMetadata addLicense(Customer customer, LicenseMetadata license) {
        try {
            LicenseGeneratorInput input = new LicenseGeneratorInput();
            input.setCustomerName(customer.getName());
            input.setPrivateKey(BaseEncoding.base64().decode(customer.getLicenseCryptDn()));
//            input.setPublicKey(BaseEncoding.base64().decode(customer.getPublicKey()));
//            input.setLicensePassword(customer.getLicensePassword());
//            input.setPrivatePassword(customer.getPrivatePassword());
//            input.setPublicPassword(customer.getPublicPassword());
            input.setThreadsCount(license.getNumberOfThreads());
            input.setLicenseType(license.getType().name());
            input.setExpiredAt(new Date()); // todo !!!

            LicenseGenerator licenseGenerator = new LicenseGenerator();
            final License generatedLicense = licenseGenerator.generate(input);
            final LdapCustomer refreshedCustomer = customerService.get(customer.getDn());

            // todo
//            refreshedCustomer.setLicenses(refreshedCustomer.getLicenses() != null ? new ArrayList<String>(refreshedCustomer.getLicenses()) : new ArrayList<String>());
//            refreshedCustomer.getLicenses().add(generatedLicense.getEncodedLicense());
            customerService.merge(refreshedCustomer);

            return license;
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException("Failed to generate license", e);
        }
    }

    private String randomPassword() {
        return RandomStringUtils.randomAlphanumeric(20);
    }

    private static LdapCustomer asLdapCustomer(Customer c) {
        LdapCustomer customer = new LdapCustomer();
        customer.setDn(c.getDn());
        customer.setId(c.getId());
        customer.setName(c.getName());
        customer.setLicenseCryptDN(c.getLicenseCryptDn());
        customer.setLicenseIdDN(toLicenseIdDNs(c.getLicenseIds()));

        // todo
//        customer.setLicenses(toLicenseList(c.getLicenses()));

        return customer;
    }

    private static List<String> toLicenseIdDNs(List<CustomerLicenseId> licenseIds) {
        List<String> result = Lists.newArrayList(); // todo
        return result;
    }

    private static List<String> toLicenseList(List<LicenseMetadata> licenses) {
        List<String> result = Lists.newArrayList(); // todo
        return result;
    }

    private static List<LicenseMetadata> toLicenses(List<String> licenses) {
        List<LicenseMetadata> result = Lists.newArrayList();    // todo
//        for (String license : licenses) {
//            final SignedLicense signedLicense = LicenseSerializationUtilities.deserialize(license);
//            final byte[] licenseContent = signedLicense.getLicenseContent();
//
//        }
        return result;
    }

    private static Customer asCustomer(LdapCustomer c) {
        Customer customer = new Customer();
        customer.setDn(c.getDn());
        customer.setId(c.getId());
        customer.setName(c.getName());
        customer.setLicenseCryptDn(c.getLicenseCryptDN());
        customer.setLicenseIds(toLicenseIds(c.getLicenseIdDN()));
        return customer;
    }

    private static List<CustomerLicenseId> toLicenseIds(List<String> licenseIdDN) {
        List<CustomerLicenseId> result = Lists.newArrayList();
        // todo
        return result;
    }


}
