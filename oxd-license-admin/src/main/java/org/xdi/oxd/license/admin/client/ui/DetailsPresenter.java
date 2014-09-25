package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import org.xdi.oxd.license.admin.client.dialogs.AddLicenseDialog;
import org.xdi.oxd.license.admin.shared.Customer;
import org.xdi.oxd.license.admin.shared.License;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 25/09/2014
 */

public class DetailsPresenter {

    private final DetailsPanel view;

    private Customer customer;

    public DetailsPresenter(DetailsPanel view) {
        this.view = view;
        view.getLicenseTable().addColumn(new TextColumn<License>() {
            @Override
            public String getValue(License license) {
                return license.getType().name();
            }
        }, "Type");
        view.getLicenseTable().addColumn(new TextColumn<License>() {
            @Override
            public String getValue(License license) {
                return Integer.toString(license.getNumberOfThreads());
            }
        }, "oxD Threads");
        view.getAddLicense().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                AddLicenseDialog dialog = new AddLicenseDialog(DetailsPresenter.this);
                dialog.show();
            }
        });
    }

    public void show(Customer customer) {
        this.customer = customer;

        view.getNameField().setHTML(asHtml(customer.getName()));
        view.getPublicKey().setHTML(asHtml(customer.getPublicKey()));
        view.getPrivateKey().setHTML(asHtml(customer.getPrivateKey()));
        view.getClientPublicKey().setHTML(asHtml(customer.getClientPublicKey()));
        view.getClientPrivateKey().setHTML(asHtml(customer.getClientPrivateKey()));
        view.getPrivatePassword().setHTML(asHtml(customer.getPrivatePassword()));
        reloadLicenseTable();
    }

    private static SafeHtml asHtml(String str) {
        return SafeHtmlUtils.fromString(str != null ? str : "");
    }

    public Customer getCustomer() {
        return customer;
    }

    public void reloadLicenseTable() {
        final List<License> licenses = customer.getLicenses() != null ? customer.getLicenses() : new ArrayList<License>();
        view.getLicenseTable().setRowData(licenses);
        view.getLicenseTable().setRowCount(licenses.size());
    }
}
