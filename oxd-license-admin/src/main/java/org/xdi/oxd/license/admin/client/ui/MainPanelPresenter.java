package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.user.client.ui.HasWidgets;
import org.xdi.oxd.license.admin.client.Presenter;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class MainPanelPresenter implements Presenter {

//    private static final Logger LOGGER = Logger.getLogger(MainPanelPresenter.class.getName());

    private final MainPanel view = new MainPanel();

    public MainPanelPresenter() {
    }

    @Override
    public void go(HasWidgets.ForIsWidget container) {
        container.clear();
        container.add(view);

        new CustomerTabPresenter(view.getCustomerTab());
    }
}
