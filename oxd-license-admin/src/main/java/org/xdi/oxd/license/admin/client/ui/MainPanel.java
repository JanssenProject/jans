package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class MainPanel implements IsWidget {

//    private static final Logger LOGGER = Logger.getLogger(MainPanel.class.getName());

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, MainPanel> {
    }

    @UiField
    DockLayoutPanel rootPanel;
    @UiField
    CustomerTab customerTab;

    public MainPanel() {
        uiBinder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return rootPanel;
    }

    public DockLayoutPanel getRootPanel() {
        return rootPanel;
    }

    public CustomerTab getCustomerTab() {
        return customerTab;
    }
}
