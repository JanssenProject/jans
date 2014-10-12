package org.xdi.oxd.license.admin.client.dialogs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.*;
import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.admin.client.framework.Framework;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.license.client.js.LicenseMetadata;
import org.xdi.oxd.license.client.js.LicenseType;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/10/2014
 */

public class LicenseIdMetadataDialog {
    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, LicenseIdMetadataDialog> {
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
    @UiField
    TextBox threadsCount;
    @UiField
    ListBox licenseType;
    @UiField
    CheckBox multiServer;
    @UiField
    HTML numberOfLicenseIdsLabel;

    private final LdapLicenseId licenseId;
    private final boolean isEditMode;

    public LicenseIdMetadataDialog(LdapLicenseId licenseId) {
        uiBinder.createAndBindUi(this);

        this.licenseId = licenseId;
        this.isEditMode = licenseId != null;

        dialog = Framework.createDialogBox("License Id configuration");
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

        for (LicenseType type : LicenseType.values()) {
            licenseType.addItem(type.getValue(), type.getValue());
        }

        setEditMode();

    }

//    private void setThreadsCount() {
//        final LicenseType type = licenseType();
//        if (type != null) {
//            this.threadsCount.setValue(Integer.toString(type.getThreadsCount()));
//        }
//    }

    private void setEditMode() {
        if (!isEditMode) {
            return;
        }

        numberOfLicenseIds.setVisible(false);
        numberOfLicenseIdsLabel.setVisible(false);

        final LicenseMetadata metadataAsObject = licenseId.getMetadataAsObject();
        if (metadataAsObject != null) {
            threadsCount.setValue(Integer.toString(metadataAsObject.getThreadsCount()));
            multiServer.setValue(metadataAsObject.isMultiServer());

            // select license type
            for (int i = 0; i < licenseType.getItemCount(); i++) {
                if (licenseType.getValue(i).equalsIgnoreCase(metadataAsObject.getLicenseType().getValue())) {
                    licenseType.setSelectedIndex(i);
                    break;
                }
            }

        }
    }

    private void showError(String message) {
        errorMessage.setVisible(true);
        errorMessage.setHTML("<span style='color:red;'>" + message + "</span>");
    }

    private boolean validate() {
        errorMessage.setVisible(false);

        final Integer numberOfLicenses = numberOfLicenses();
        final Integer threadsCount = threadsCount();
        final LicenseType licenseType = licenseType();

        if ((numberOfLicenses == null || numberOfLicenses <= 0) && !isEditMode) {
            showError("Unable to parse number of licenses. Please enter integer more then zero.");
            return false;
        }
        if (threadsCount == null || threadsCount <= 0) {
            showError("Unable to parse number of threads.");
            return false;
        }
        if (licenseType == null) {
            showError("Unable to identify license type.");
            return false;
        }

        return true;
    }

    public LicenseMetadata licenseMetadata() {
        return new LicenseMetadata()
                .setLicenseType(licenseType())
                .setMultiServer(multiServer.getValue())
                .setThreadsCount(threadsCount());
    }

    public LicenseType licenseType() {
        final int selectedIndex = licenseType.getSelectedIndex();
        if (selectedIndex != -1) {
            return LicenseType.fromValue(licenseType.getValue(selectedIndex));
        }
        return null;
    }

    public Integer numberOfLicenses() {
        return Admin.parse(numberOfLicenseIds.getValue());
    }

    public Integer threadsCount() {
        return Admin.parse(threadsCount.getValue());
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

    public LdapLicenseId getLicenseId() {
        if (isEditMode) {
            licenseId.setForceLicenseUpdate(true);
            licenseId.setMetadataAsObject(licenseMetadata());
        }
        return licenseId;
    }
}
