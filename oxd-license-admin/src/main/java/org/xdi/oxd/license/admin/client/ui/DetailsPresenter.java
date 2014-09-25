package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import org.xdi.oxd.license.admin.shared.Customer;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 25/09/2014
 */

public class DetailsPresenter {

    private final DetailsPanel view;

    public DetailsPresenter(DetailsPanel view) {
        this.view = view;
    }

    public void show(Customer customer) {
        view.getNameField().setHTML(asHtml(customer.getName()));
        view.getPublicKey().setHTML(asHtml(customer.getPublicKey()));
        view.getPrivateKey().setHTML(asHtml(customer.getPrivateKey()));
        view.getClientPublicKey().setHTML(asHtml(customer.getClientPublicKey()));
        view.getClientPrivateKey().setHTML(asHtml(customer.getClientPrivateKey()));
        view.getPrivatePassword().setHTML(asHtml(customer.getPrivatePassword()));
    }

    private static SafeHtml asHtml(String str) {
        return SafeHtmlUtils.fromString(str != null ? str : "");
    }

}
