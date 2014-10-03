package org.xdi.oxd.license.admin.client.ui;

import org.xdi.oxd.license.admin.client.Admin;
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

    public void show(LdapCustomer entity) {
        if (entity == null) {
            clear();
        }

        view.getNameField().setHTML(Admin.asHtml(entity.getName()));

        // todo
    }

    private void clear() {
        view.getNameField().setHTML("");
    }
}
