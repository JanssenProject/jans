package org.xdi.oxd.license.admin.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import org.xdi.oxd.license.admin.client.service.AdminService;
import org.xdi.oxd.license.admin.shared.Customer;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class AdminServiceImpl extends RemoteServiceServlet implements AdminService {
    @Override
    public List<Customer> getCustomers() {
        return null;
    }

    @Override
    public void save(Customer customer) {

    }
}
