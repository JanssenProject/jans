package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Label;
import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.admin.client.Presenter;
import org.xdi.oxd.license.admin.client.SuccessCallback;
import org.xdi.oxd.license.admin.client.dialogs.EditCustomerDialog;
import org.xdi.oxd.license.admin.shared.Customer;

import java.util.List;
import java.util.logging.Logger;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class MainPanelPresenter implements Presenter {

    private static final Logger LOGGER = Logger.getLogger(MainPanelPresenter.class.getName());

    private final MainPanel view = new MainPanel();

    public MainPanelPresenter() {
    }

    @Override
    public void go(HasWidgets.ForIsWidget container) {
        container.clear();
        container.add(view);

        view.getTable().setEmptyTableWidget(new Label("No data"));
        Admin.getService().getCustomers(new SuccessCallback<List<Customer>>() {
            @Override
            public void onSuccess(List<Customer> result) {
                view.getTable().setRowData(result);
            }
        });

        view.getAddButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                EditCustomerDialog dialog = new EditCustomerDialog();
                dialog.setTitle("Create customer");
                dialog.show();
            }
        });
    }
}
