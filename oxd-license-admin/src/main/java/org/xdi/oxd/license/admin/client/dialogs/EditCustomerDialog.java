package org.xdi.oxd.license.admin.client.dialogs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/09/2014
 */

public class EditCustomerDialog implements IsWidget {

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<DialogBox, EditCustomerDialog> {
    }

    @UiField
    DialogBox dialog;
    @UiField
    TextBox nameField;
    @UiField
    HTML title;
    @UiField
    Button closeButton;
    @UiField
    Button okButton;

    public EditCustomerDialog() {
        uiBinder.createAndBindUi(this);
        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                dialog.hide(true);
            }
        });
    }

    public void setTitle(String title) {
        this.title.setHTML(title);
    }

    public void show() {
        dialog.center();
        dialog.show();
    }

    @Override
    public Widget asWidget() {
        return dialog;
    }

}
