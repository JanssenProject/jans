package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/10/2014
 */

public class CustomerDetailsPanel implements IsWidget {

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, CustomerDetailsPanel> {
    }

    @UiField
    VerticalPanel rootPanel;
    @UiField
    HTML nameField;
    @UiField
    HTML cryptNameField;

    public CustomerDetailsPanel() {
        uiBinder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return rootPanel;
    }

    public HTML getCryptNameField() {
        return cryptNameField;
    }

    public HTML getNameField() {
        return nameField;
    }
}
