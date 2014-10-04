package org.xdi.oxd.license.admin.client.ui;

import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.admin.client.SuccessCallback;
import org.xdi.oxd.license.client.js.LdapCustomer;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;

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
            return;
        }

        view.getNameField().setHTML(Admin.asHtml(entity.getName()));
        Admin.getService().getLicenseCrypt(entity.getDn(), new SuccessCallback<LdapLicenseCrypt>() {
            @Override
            public void onSuccess(LdapLicenseCrypt result) {
                view.getCryptNameField().setHTML(Admin.asHtml(result.getName()));
            }
        });
    }

    private void clear() {
        view.getNameField().setHTML("");
        view.getCryptNameField().setHTML("");
    }
}
