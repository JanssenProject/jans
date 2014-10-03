package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.admin.client.SuccessCallback;
import org.xdi.oxd.license.admin.client.dialogs.EditCustomerDialog;
import org.xdi.oxd.license.admin.shared.Customer;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 01/10/2014
 */

public class CustomerTabPresenter {

    private final CustomerTab view;
    private final SingleSelectionModel<Customer> selectionModel = new SingleSelectionModel<Customer>();
    private final LicenseCryptDetailsPresenter detailsPresenter;

    public CustomerTabPresenter(CustomerTab view) {
        this.view = view;
        this.detailsPresenter = new LicenseCryptDetailsPresenter(view.getDetailsPanel());

        view.getAddButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                EditCustomerDialog dialog = new EditCustomerDialog() {
                    @Override
                    public void onSuccess() {
                        loadCustomers();
                    }
                };
                dialog.setTitle("Create customer");
                dialog.show();
            }
        });
        view.getRefreshButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                loadCustomers();
            }
        });
        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                detailsPresenter.show(selectionModel.getSelectedObject());
            }
        });

        view.getTable().setEmptyTableWidget(new Label("No data"));
        view.getTable().setSelectionModel(selectionModel);

        loadCustomers();

    }

    private void loadCustomers() {
        Admin.getService().getCustomers(new SuccessCallback<List<Customer>>() {
            @Override
            public void onSuccess(List<Customer> result) {
                view.getTable().setRowData(result);
                view.getTable().setRowCount(result.size());
            }
        });
    }
}
