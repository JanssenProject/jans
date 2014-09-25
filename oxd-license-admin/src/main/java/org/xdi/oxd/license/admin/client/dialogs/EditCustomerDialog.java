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
import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.admin.client.SuccessCallback;
import org.xdi.oxd.license.admin.shared.Customer;
import org.xdi.oxd.license.admin.shared.GeneratedKeys;

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
    @UiField
    HTML errorMessage;
    @UiField
    HTML privateKey;
    @UiField
    HTML publicKey;
    @UiField
    Button generateButton;
    @UiField
    HTML licensePassword;
    @UiField
    HTML publicPassword;
    @UiField
    HTML privatePassword;

    private GeneratedKeys generatedKeys;

    public EditCustomerDialog() {
        uiBinder.createAndBindUi(this);
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
        generateButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                generateKeys();
            }
        });
    }

    private void generateKeys() {
        Admin.getService().generateKeys(new SuccessCallback<GeneratedKeys>() {
            @Override
            public void onSuccess(GeneratedKeys result) {
                generatedKeys = result;
                privateKey.setText(result.getPrivateKey());
                publicKey.setText(result.getPublicKey());
                privatePassword.setText(result.getPrivatePassword());
                publicPassword.setText(result.getPublicPassword());
                licensePassword.setText(result.getLicensePassword());
            }
        });
    }

    private boolean validate() {
        errorMessage.setVisible(false);

        if (Admin.isEmpty(nameField.getValue())) {
            showError("Name is blank.");
            return false;
        }
        if (Admin.isEmpty(privateKey.getText())) {
            showError("Private key and passwords are not generated. Please hit generate button.");
            return false;
        }
        return true;
    }

    private void showError(String message) {
        errorMessage.setVisible(true);
        errorMessage.setHTML("<span style='color:red;'>" + message + "</span>");
    }

    private void onOkClick() {
        final Customer customer = new Customer();
        customer.setName(nameField.getValue());
        customer.setLicensePassword(generatedKeys.getLicensePassword());
        customer.setPrivatePassword(generatedKeys.getPrivatePassword());
        customer.setPublicPassword(generatedKeys.getPublicPassword());
        customer.setPublicKey(generatedKeys.getPublicKey());
        customer.setPrivateKey(generatedKeys.getPrivateKey());
        Admin.getService().create(customer, new SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                dialog.hide();
                EditCustomerDialog.this.onSuccess();
            }
        });
    }

    public void onSuccess() {

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
