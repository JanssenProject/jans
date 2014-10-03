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
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 03/10/2014
 */

public class AddLicenseCryptDialog {

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, AddLicenseCryptDialog> {
    }

    @UiField
    VerticalPanel dialogContent;
    @UiField
    TextBox nameField;
    @UiField
    Button generateButton;
    @UiField
    HTML privateKey;
    @UiField
    HTML publicKey;
    @UiField
    HTML privatePassword;
    @UiField
    HTML publicPassword;
    @UiField
    HTML licensePassword;
    @UiField
    HTML errorMessage;
    @UiField
    Button okButton;
    @UiField
    Button closeButton;

    private final DialogBox dialog;
    private LdapLicenseCrypt licenseCrypt;

    public AddLicenseCryptDialog(LdapLicenseCrypt licenseCrypt) {
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
                    save();
                }
            }
        });
        generateButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                generateLicenseCrypt();
            }
        });

        generateButton.setEnabled(licenseCrypt == null);
        this.licenseCrypt = licenseCrypt;
        if (licenseCrypt != null) {
            updateUI(licenseCrypt);
        }
    }

    private void generateLicenseCrypt() {
        Admin.getService().generate(new SuccessCallback<LdapLicenseCrypt>() {
            @Override
            public void onSuccess(LdapLicenseCrypt result) {
                licenseCrypt = result;
                licenseCrypt.setName(nameField.getValue());
                updateUI(result);
            }
        });
    }

    private void updateUI(LdapLicenseCrypt result) {
        nameField.setValue(result.getName());
        privateKey.setText(result.getPrivateKey());
        publicKey.setText(result.getPublicKey());
        privatePassword.setText(result.getPrivatePassword());
        publicPassword.setText(result.getPublicPassword());
        licensePassword.setText(result.getLicensePassword());
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

    private void save() {
        Admin.getService().save(licenseCrypt, new SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                dialog.hide();
                AddLicenseCryptDialog.this.onSuccess();
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
