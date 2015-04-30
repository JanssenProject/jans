package org.xdi.oxd.license.admin.server;

import com.google.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.base.Strings;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.license.admin.client.service.AdminService;
import org.xdi.oxd.license.client.Jackson;
import org.xdi.oxd.license.client.js.LdapCustomer;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.license.client.js.LicenseMetadata;
import org.xdi.oxd.license.client.js.Configuration;
import org.xdi.oxd.licenser.server.service.CustomerService;
import org.xdi.oxd.licenser.server.service.LicenseCryptService;
import org.xdi.oxd.licenser.server.service.LicenseIdService;

import java.util.ArrayList;
import java.util.Collection;
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
    @Inject
    LicenseIdService licenseIdService;
    @Inject
    Configuration conf;

    @Override
    public Configuration getConfiguration() {
        return conf;
    }

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
        return licenseCryptService.generate();
    }

    @Override
    public List<LdapLicenseId> generateLicenseIds(int count, LdapLicenseCrypt licenseCrypt, LicenseMetadata metadata) {
        List<LdapLicenseId> result = new ArrayList<LdapLicenseId>();
        for (int i = 0; i < count; i++) {
            final LdapLicenseId entity = licenseIdService.generate(licenseCrypt.getDn(), metadata);
            licenseIdService.save(entity);
            result.add(entity);
        }
        return result;
    }

//    public LicenseMetadata addLicense(Customer customer, LicenseMetadata license) {
//        try {
//            LicenseGeneratorInput input = new LicenseGeneratorInput();
//            input.setCustomerName(customer.getName());
//            input.setPrivateKey(BaseEncoding.base64().decode(customer.getLicenseCryptDn()));
////            input.setPublicKey(BaseEncoding.base64().decode(customer.getPublicKey()));
////            input.setLicensePassword(customer.getLicensePassword());
////            input.setPrivatePassword(customer.getPrivatePassword());
////            input.setPublicPassword(customer.getPublicPassword());
//           /* input.setThreadsCount(license.getNumberOfThreads());
//            input.setLicenseType(license.getType().name());*/
//            input.setExpiredAt(new Date());
//
//            LicenseGenerator licenseGenerator = new LicenseGenerator();
//            final License generatedLicense = licenseGenerator.generate(input);
//            final LdapCustomer refreshedCustomer = customerService.get(customer.getDn());
//
//
////            refreshedCustomer.setLicenses(refreshedCustomer.getLicenses() != null ? new ArrayList<String>(refreshedCustomer.getLicenses()) : new ArrayList<String>());
////            refreshedCustomer.getLicenses().add(generatedLicense.getEncodedLicense());
//            customerService.merge(refreshedCustomer);
//
//            return license;
//        } catch (Exception e) {
//            LOG.error(e.getMessage(), e);
//            throw new RuntimeException("Failed to generate license", e);
//        }
//    }

    @Override
    public List<LdapLicenseId> loadLicenseIdsByCrypt(LdapLicenseCrypt licenseCrypt) {
        return licenseIdService.getByCryptDn(licenseCrypt.getDn());
    }

    @Override
    public LdapLicenseCrypt getLicenseCrypt(String dn) {
        return licenseCryptService.get(dn);
    }

    @Override
    public void save(LdapLicenseId entity) {
        final LicenseMetadata metadataAsObject = entity.getMetadataAsObject();
        if (metadataAsObject != null) {
            entity.setMetadata(Jackson.asJsonSilently(metadataAsObject));
        }

        if (Strings.isNullOrEmpty(entity.getDn())) {
            licenseIdService.save(entity);
        } else {
            licenseIdService.merge(entity);
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
        remove(loadLicenseIdsByCrypt(entity));
    }

    @Override
    public void remove(Collection<LdapLicenseId> entities) {
        for (LdapLicenseId entry : entities) {
            licenseIdService.remove(entry);
        }
    }

    @Override
    public List<LdapLicenseCrypt> getAllLicenseCryptObjects() {
        try {
            return licenseCryptService.getAll();
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return Lists.newArrayList();
        }
    }


}
