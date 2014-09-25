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

    public DetailsPanel() {
        uiBinder.createAndBindUi(this);
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
}
