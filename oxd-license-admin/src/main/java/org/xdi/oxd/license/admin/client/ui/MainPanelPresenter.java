package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;
import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.admin.client.Presenter;
import org.xdi.oxd.license.admin.client.SuccessCallback;
import org.xdi.oxd.license.admin.client.dialogs.EditCustomerDialog;
import org.xdi.oxd.license.admin.shared.Customer;

import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class MainPanelPresenter implements Presenter {

//    private static final Logger LOGGER = Logger.getLogger(MainPanelPresenter.class.getName());

    private final MainPanel view = new MainPanel();
    private final SingleSelectionModel<Customer> selectionModel = new SingleSelectionModel<Customer>();
    private final DetailsPresenter detailsPresenter = new DetailsPresenter(view.getDetailsPanel());

    public MainPanelPresenter() {
    }

    @Override
    public void go(HasWidgets.ForIsWidget container) {
        container.clear();
        container.add(view);

        view.getTable().setEmptyTableWidget(new Label("No data"));
        view.getTable().setSelectionModel(selectionModel);
        loadCustomers();

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
