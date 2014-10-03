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
import org.xdi.oxd.license.admin.client.SuccessCallback;
import org.xdi.oxd.license.admin.client.framework.Framework;
import org.xdi.oxd.license.client.js.LdapCustomer;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;

import java.util.ArrayList;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/09/2014
 */

public class AddCustomerDialog {

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, AddCustomerDialog> {
    }

    private final DialogBox dialog;

    @UiField
    TextBox nameField;
    @UiField
    Button closeButton;
    @UiField
    Button okButton;
    @UiField
    HTML errorMessage;
    @UiField
    VerticalPanel dialogContent;
    @UiField
    Button selectCryptButton;
    @UiField
    HTML crypt;

    LdapLicenseCrypt licenseCrypt;

    public AddCustomerDialog() {
        uiBinder.createAndBindUi(this);

        dialog = Framework.createDialogBox("Add Customer");
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
                    onOkClick();
                }
            }
        });

        selectCryptButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                SelectCryptDialog dialog = new SelectCryptDialog() {
                    @Override
                    public void onOk() {
                        licenseCrypt = getLdapLicenseCrypt();
                        crypt.setText(licenseCrypt.getName());
                    }
                };
                dialog.show();
            }
        });
    }

    private boolean validate() {
        errorMessage.setVisible(false);

        if (Admin.isEmpty(nameField.getValue())) {
            showError("Name is blank.");
            return false;
        }
        if (licenseCrypt == null) {
            showError("Crypt keys are not selected.");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        errorMessage.setVisible(true);
        errorMessage.setHTML("<span style='color:red;'>" + message + "</span>");
    }

    private void onOkClick() {
        final LdapCustomer customer = new LdapCustomer();
        customer.setName(nameField.getValue());
        customer.setLicenseCryptDN(licenseCrypt.getDn());
        customer.setLicenseIdDN(new ArrayList<String>());

        Admin.getService().save(customer, new SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                dialog.hide();
                AddCustomerDialog.this.onSuccess();
            }
        });
    }

    public void onSuccess() {

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
