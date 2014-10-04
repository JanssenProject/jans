package org.xdi.oxd.license.admin.client.service;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.xdi.oxd.license.admin.shared.Customer;
import org.xdi.oxd.license.admin.shared.LicenseMetadata;
import org.xdi.oxd.license.client.js.LdapCustomer;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;
import org.xdi.oxd.license.client.js.LdapLicenseId;

import java.util.List;

public interface AdminServiceAsync {
    void getAllCustomers(AsyncCallback<List<LdapCustomer>> async);

    void save(LdapCustomer customer, AsyncCallback<Void> async);

    void save(LdapLicenseCrypt customer, AsyncCallback<Void> async);

    void generate(AsyncCallback<LdapLicenseCrypt> async);

    void addLicense(Customer customer, LicenseMetadata license, AsyncCallback<LicenseMetadata> async);

    void getAllLicenseCrypts(AsyncCallback<List<LdapLicenseCrypt>> async);

    void remove(LdapCustomer entity, AsyncCallback<Void> async);

    void remove(LdapLicenseCrypt entity, AsyncCallback<Void> async);

    void generateLicenseIds(int count, LdapLicenseCrypt licenseCrypt, AsyncCallback<List<LdapLicenseId>> async);

    void loadLicenseIdsByCrypt(LdapLicenseCrypt licenseCrypt, AsyncCallback<List<LdapLicenseId>> async);
}
