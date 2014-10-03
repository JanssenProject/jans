package org.xdi.oxd.license.admin.server;

import com.google.common.collect.Lists;
import com.google.common.io.BaseEncoding;
import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.nicholaswilliams.java.licensing.encryption.RSAKeyPairGenerator;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.license.admin.client.service.AdminService;
import org.xdi.oxd.license.admin.shared.Customer;
import org.xdi.oxd.license.admin.shared.LicenseMetadata;
import org.xdi.oxd.license.client.data.License;
import org.xdi.oxd.license.client.js.LdapCustomer;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;
import org.xdi.oxd.licenser.server.LicenseGenerator;
import org.xdi.oxd.licenser.server.LicenseGeneratorInput;
import org.xdi.oxd.licenser.server.LicenseSerializationUtilities;
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
    public List<LdapCustomer> getAllCustomers() {
        return customerService.getAll();
    }

    @Override
    public void save(LdapCustomer entity) {
        if (Strings.isNullOrEmpty(entity.getDn())) {
            customerService.save(entity);
        } else {
            customerService.merge(entity);
        }
    }

    @Override
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

    @Override
    public void save(LdapLicenseCrypt entity) {
        if (Strings.isNullOrEmpty(entity.getDn())) {
            licenseCryptService.save(entity);
        } else {
            licenseCryptService.merge(entity);
        }
    }

    @Override
    public void remove(LdapCustomer entity) {
        customerService.remove(entity);
    }

    @Override
    public void remove(LdapLicenseCrypt entity) {
        licenseCryptService.remove(entity);
    }

    @Override
    public List<LdapLicenseCrypt> getAllLicenseCrypts() {
        try {
            return licenseCryptService.getAll();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Lists.newArrayList();
        }
    }

    private String randomPassword() {
        return RandomStringUtils.randomAlphanumeric(20);
    }


}
