package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.license.client.js.LicenseMetadata;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/09/2014
 */

public class LicenseCryptDetailsPanel implements IsWidget {

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);


    interface MyUiBinder extends UiBinder<Widget, LicenseCryptDetailsPanel> {
    }

    @UiField
    HTML nameField;
    @UiField
    Widget rootPanel;
    @UiField
    HTML privatePassword;
    //    @UiField
    HTML clientPublicKey = new HTML();
    //    @UiField
    HTML clientPrivateKey = new HTML();
    @UiField
    HTML publicKey;
    @UiField
    HTML privateKey;
    @UiField
    HTML licensePassword;
    @UiField
    HTML publicPassword;
    @UiField
    Button generateLicenseIdButton;
    @UiField
    HTML licenseIdCount;
    @UiField
    CellTable<LdapLicenseId> licenseIds;
    @UiField
    Button removeButton;
    @UiField
    Button editButton;
    @UiField
    Button refreshButton;

    public LicenseCryptDetailsPanel() {
        uiBinder.createAndBindUi(this);
        licenseIds.setEmptyTableWidget(new Label("No data"));
        licenseIds.addColumn(new TextColumn<LdapLicenseId>() {
            @Override
            public String getValue(LdapLicenseId object) {
                return object.getLicenseId();
            }
        }, "License Id");
        licenseIds.addColumn(new TextColumn<LdapLicenseId>() {
            @Override
            public String getValue(LdapLicenseId object) {
                final LicenseMetadata m = object.getMetadataAsObject();
                if (m != null && m.getLicenseType() != null) {
                    return m.getLicenseType().getValue();
                }
                return "";
            }
        }, "Type");
        licenseIds.addColumn(new TextColumn<LdapLicenseId>() {
            @Override
            public String getValue(LdapLicenseId object) {
                if (object.getLicensesIssuedCount() != null) {
                    return Integer.toString(object.getLicensesIssuedCount());
                }
                return "0";
            }
        }, "Licenses issued");
        licenseIds.addColumn(new TextColumn<LdapLicenseId>() {
            @Override
            public String getValue(LdapLicenseId object) {
                final LicenseMetadata m = object.getMetadataAsObject();
                if (m != null) {
                    return Integer.toString(m.getThreadsCount());
                }
                return "";
            }
        }, "Threads count");
        licenseIds.addColumn(new TextColumn<LdapLicenseId>() {
            @Override
            public String getValue(LdapLicenseId object) {
                final LicenseMetadata m = object.getMetadataAsObject();
                if (m != null) {
                    return Boolean.toString(m.isMultiServer());
                }
                return "";
            }
        }, "Multi-server");

    }

    public Button getEditButton() {
        return editButton;
    }

    public Button getRemoveButton() {
        return removeButton;
    }

    public CellTable<LdapLicenseId> getLicenseIds() {
        return licenseIds;
    }

    public HTML getLicensePassword() {
        return licensePassword;
    }

    public Button getRefreshButton() {
        return refreshButton;
    }

    public HTML getPublicPassword() {
        return publicPassword;
    }

    public HTML getLicenseIdCount() {
        return licenseIdCount;
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

    public Button getGenerateLicenseIdButton() {
        return generateLicenseIdButton;
    }

    public HTML getPrivatePassword() {
        return privatePassword;
    }

    public HTML getPublicKey() {
        return publicKey;
    }
}
