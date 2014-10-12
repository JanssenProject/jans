package org.xdi.oxd.license.admin.client.service;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import org.xdi.oxd.license.client.js.LdapCustomer;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.license.client.js.LicenseMetadata;

import java.util.Collection;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

@RemoteServiceRelativePath("adminService.rpc")
public interface AdminService extends RemoteService {

    public List<LdapCustomer> getAllCustomers();

    public List<LdapLicenseCrypt> getAllLicenseCrypts();

    public void save(LdapCustomer entity);

    public void save(LdapLicenseCrypt entity);

    public void remove(LdapCustomer entity);

    public void remove(LdapLicenseCrypt entity);

    public void remove(Collection<LdapLicenseId> entities);

    public LdapLicenseCrypt generate();

    public List<LdapLicenseId> generateLicenseIds(int count, LdapLicenseCrypt licenseCrypt, LicenseMetadata metadata);

    List<LdapLicenseId> loadLicenseIdsByCrypt(LdapLicenseCrypt licenseCrypt);

    public LdapLicenseCrypt getLicenseCrypt(String dn);
}
