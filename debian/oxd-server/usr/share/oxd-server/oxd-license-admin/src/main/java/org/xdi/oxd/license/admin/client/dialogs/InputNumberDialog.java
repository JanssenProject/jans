package org.xdi.oxd.license.admin.client.dialogs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.admin.client.framework.Framework;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 04/10/2014
 */

public class InputNumberDialog {

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, InputNumberDialog> {
    }

    private final DialogBox dialog;

    @UiField
    VerticalPanel dialogContent;
    @UiField
    HTML errorMessage;
    @UiField
    Button okButton;
    @UiField
    Button closeButton;
    @UiField
    TextBox numberOfLicenseIds;

    public InputNumberDialog() {
        uiBinder.createAndBindUi(this);

        dialog = Framework.createDialogBox("");
        dialog.setWidget(dialogContent);

        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                dialog.hide();
            }
        });
        okButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (validate()) {
                    dialog.hide();
                    onOk();
                }
            }
        });

        setTitle("Enter number of License Ids ");
    }

    private void showError(String message) {
        errorMessage.setVisible(true);
        errorMessage.setHTML("<span style='color:red;'>" + message + "</span>");
    }

    private boolean validate() {
        errorMessage.setVisible(false);

        final Integer numberOfLicenses = numberOfLicenses();
        if (numberOfLicenses == null || numberOfLicenses <=0) {
            showError("Unable to parse number of licenses. Please enter integer more then zero.");
            return false;
        }
        return true;
    }

    public Integer numberOfLicenses() {
        return Admin.parse(numberOfLicenseIds.getValue());
    }

    public void onOk() {
    }

    public void setTitle(String title) {
        dialog.setText(title);
        dialog.setTitle(title);
    }

    public void show() {
        dialog.center();
        dialog.show();
    }
}
