package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.xdi.oxd.license.admin.client.dialogs.AddLicenseDialog;
import org.xdi.oxd.license.admin.shared.Customer;
import org.xdi.oxd.license.admin.shared.LicenseMetadata;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 25/09/2014
 */

public class DetailsPresenter {

    private final DetailsPanel view;

    private Customer customer;
    private SingleSelectionModel<LicenseMetadata> selectionModel = new SingleSelectionModel<LicenseMetadata>();

    public DetailsPresenter(DetailsPanel view) {
        this.view = view;
        configureLicenseTable(view);

        view.getAddLicense().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                AddLicenseDialog dialog = new AddLicenseDialog(DetailsPresenter.this);
                dialog.show();
            }
        });
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                DetailsPresenter.this.view.getRemoveLicense().setEnabled(selectionModel.getSelectedObject() != null);
            }
        });
    }

    private void configureLicenseTable(DetailsPanel view) {
        view.getLicenseTable().setSelectionModel(selectionModel);
        view.getLicenseTable().addColumn(new TextColumn<LicenseMetadata>() {
            @Override
            public String getValue(LicenseMetadata license) {
                return license.getType().name();
            }
        }, "Type");
        view.getLicenseTable().addColumn(new TextColumn<LicenseMetadata>() {
            @Override
            public String getValue(LicenseMetadata license) {
                return Integer.toString(license.getNumberOfThreads());
            }
        }, "oxD Threads");

        view.getLicenseTable().setColumnWidth(0, 70, Style.Unit.PX);
        view.getLicenseTable().setColumnWidth(1, 200, Style.Unit.PX);
    }

    public void show(Customer customer) {
        this.customer = customer;

        view.getAddLicense().setEnabled(true);
        view.getNameField().setHTML(asHtml(customer.getName()));
        view.getPublicKey().setHTML(asHtml(customer.getPublicKey()));
        view.getPrivateKey().setHTML(asHtml(customer.getPrivateKey()));
        view.getClientPublicKey().setHTML(asHtml(customer.getClientPublicKey()));
        view.getClientPrivateKey().setHTML(asHtml(customer.getClientPrivateKey()));
        view.getPrivatePassword().setHTML(asHtml(customer.getPrivatePassword()));
        view.getPublicPassword().setHTML(asHtml(customer.getPublicPassword()));
        view.getLicensePassword().setHTML(asHtml(customer.getLicensePassword()));
        reloadLicenseTable();
    }

    private static SafeHtml asHtml(String str) {
        String s = str != null ? str : "";
        if (s.length() > 40) {
            s = s.substring(0, 40) + "...";
        }
        return SafeHtmlUtils.fromString(s);
    }

    public Customer getCustomer() {
        return customer;
    }

    public void reloadLicenseTable() {
        // todo
//        final List<LicenseMetadata> licenses = customer.getLicenseIds() != null ? customer.getLicenseIds() : new ArrayList<LicenseMetadata>();
//        view.getLicenseTable().setRowData(licenses);
//        view.getLicenseTable().setRowCount(licenses.size());
    }
}
