package org.xdi.oxd.license.admin.client.service;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import org.xdi.oxd.license.admin.shared.Customer;
import org.xdi.oxd.license.admin.shared.GeneratedKeys;
import org.xdi.oxd.license.admin.shared.License;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

@RemoteServiceRelativePath("adminService.rpc")
public interface AdminService extends RemoteService {

    public List<Customer> getCustomers();

    public void save(Customer customer);

    public void create(Customer customer);

    public GeneratedKeys generateKeys();

    public License addLicense(Customer customer, License license);
}
