package org.xdi.oxd.license.admin.client.service;

import com.google.gwt.user.client.rpc.AsyncCallback;
import org.xdi.oxd.license.admin.shared.Customer;

import java.util.List;

public interface AdminServiceAsync {
    void getCustomers(AsyncCallback<List<Customer>> async);

    void save(Customer customer, AsyncCallback<Void> async);

    void create(Customer customer, AsyncCallback<Void> async);
}
