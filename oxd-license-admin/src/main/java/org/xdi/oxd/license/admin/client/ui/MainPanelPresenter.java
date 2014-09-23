package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.user.client.ui.HasWidgets;
import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.admin.client.Presenter;
import org.xdi.oxd.license.admin.client.SuccessCallback;
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
        Admin.getService().getCustomers(new SuccessCallback<List<Customer>>() {
            @Override
            public void onSuccess(List<Customer> result) {
                view.getTable().setRowData(result);
            }
        });
    }
}
