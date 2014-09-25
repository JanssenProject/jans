package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import org.xdi.oxd.license.admin.shared.CustomerLicense;

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
    @UiField
    HTML privatePassword;
    @UiField
    HTML clientPublicKey;
    @UiField
    HTML clientPrivateKey;
    @UiField
    HTML publicKey;
    @UiField
    HTML privateKey;
    @UiField
    CellTable<CustomerLicense> licenseTable;
    @UiField
    Button removeLicense;
    @UiField
    Button addLicense;
    @UiField
    HTML licensePassword;
    @UiField
    HTML publicPassword;

    public DetailsPanel() {
        uiBinder.createAndBindUi(this);
    }

    public Button getAddLicense() {
        return addLicense;
    }

    public Button getRemoveLicense() {
        return removeLicense;
    }

    public HTML getLicensePassword() {
        return licensePassword;
    }

    public HTML getPublicPassword() {
        return publicPassword;
    }

    @Override
    public Widget asWidget() {
        return rootPanel;
    }

    public HTML getNameField() {
        return nameField;
    }

    public HTML getClientPrivateKey() {
        return clientPrivateKey;
    }

    public HTML getClientPublicKey() {
        return clientPublicKey;
    }

    public HTML getPrivateKey() {
        return privateKey;
    }

    public HTML getPrivatePassword() {
        return privatePassword;
    }

    public HTML getPublicKey() {
        return publicKey;
    }

    public CellTable<CustomerLicense> getLicenseTable() {
        return licenseTable;
    }
}
