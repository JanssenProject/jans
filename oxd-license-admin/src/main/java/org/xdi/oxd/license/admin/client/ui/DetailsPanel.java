package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.xdi.oxd.license.admin.shared.Customer;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class DetailsPanel implements IsWidget {

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<VerticalPanel, DetailsPanel> {
    }

    @UiField
    HTML nameField;
    @UiField
    VerticalPanel rootPanel;

    public DetailsPanel() {
        uiBinder.createAndBindUi(this);
    }

    @Override
    public Widget asWidget() {
        return rootPanel;
    }

    public void show(Customer customer) {
        nameField.setHTML(asHtml(customer.getName()));
    }

    private static SafeHtml asHtml(String str) {
        return SafeHtmlUtils.fromString(str != null ? str : "");
    }
}
