package org.xdi.oxd.license.admin.client.ui;

import org.xdi.oxd.license.client.js.LdapCustomer;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/10/2014
 */

public class CustomerDetailsPresenter {

    CustomerDetailsPanel view;

    public CustomerDetailsPresenter(CustomerDetailsPanel detailsPanel) {
        this.view = detailsPanel;
    }

    public void show(LdapCustomer selectedObject) {
    }
}
