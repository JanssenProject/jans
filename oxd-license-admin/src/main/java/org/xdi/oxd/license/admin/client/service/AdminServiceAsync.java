package org.xdi.oxd.license.admin.client.service;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.xdi.oxd.license.client.js.LdapCustomer;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.license.client.js.LicenseMetadata;

import java.util.Collection;
import java.util.List;

public interface AdminServiceAsync {
    void getAllCustomers(AsyncCallback<List<LdapCustomer>> async);

    void save(LdapCustomer customer, AsyncCallback<Void> async);

    void save(LdapLicenseCrypt customer, AsyncCallback<Void> async);

    void save(LdapLicenseId entity, AsyncCallback<Void> async);

    void generate(AsyncCallback<LdapLicenseCrypt> async);

    void getAllLicenseCryptObjects(AsyncCallback<List<LdapLicenseCrypt>> async);

    void removeCustomer(LdapCustomer entity, AsyncCallback<Void> async);

    void removeCrypt(LdapLicenseCrypt entity, AsyncCallback<Void> async);

    void generateLicenseIds(int count, LdapLicenseCrypt licenseCrypt, LicenseMetadata metadata, AsyncCallback<List<LdapLicenseId>> async);

    void loadLicenseIdsByCrypt(LdapLicenseCrypt licenseCrypt, AsyncCallback<List<LdapLicenseId>> async);

    void getLicenseCrypt(String dn, AsyncCallback<LdapLicenseCrypt> async);

    void remove(Collection<LdapLicenseId> entities, AsyncCallback<Void> async);

    void getLoginUrl(AsyncCallback<String> async);

    void getLogoutUrl(AsyncCallback<String> async);
}
