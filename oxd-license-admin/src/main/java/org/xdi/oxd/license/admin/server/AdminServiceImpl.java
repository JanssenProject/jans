package org.xdi.oxd.license.admin.server;

import com.google.common.collect.Lists;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.xdi.oxd.license.admin.client.service.AdminService;
import org.xdi.oxd.license.admin.shared.Customer;
import org.xdi.oxd.licenser.server.ldap.LdapCustomer;
import org.xdi.oxd.licenser.server.persistence.CustomerService;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

@Singleton
public class AdminServiceImpl extends RemoteServiceServlet implements AdminService {

    @Inject
    CustomerService customerService;

    @Override
    public List<Customer> getCustomers() {
        return Lists.newArrayList();
    }

    @Override
    public void save(Customer customer) {

    }

    @Override
    public void create(Customer customer) {
        customerService.save(asLdapCustomer(customer));
    }

    private static LdapCustomer asLdapCustomer(Customer customer) {
        LdapCustomer ldapCustomer = new LdapCustomer();
        // todo
        return ldapCustomer;
    }
}
