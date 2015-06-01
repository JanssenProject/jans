package org.xdi.oxd.license.admin.client.dialogs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.*;
import com.google.gwt.user.datepicker.client.DateBox;
import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.admin.client.framework.Framework;
import org.xdi.oxd.license.client.js.Configuration;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.license.client.js.LicenseMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 06/10/2014
 */

public class LicenseIdMetadataDialog {

    private static final Logger LOGGER = Logger.getLogger(LicenseIdMetadataDialog.class.getName());

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);

    interface MyUiBinder extends UiBinder<Widget, LicenseIdMetadataDialog> {
    }

    private static final List<String> FALLBACK_FEATURES = Arrays.asList(
            "gluu_server", "cas", "shib_idp", "mod_ox", "nginx"
    );

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
    CheckBox multiServer;
    @UiField
    HTML numberOfLicenseIdsLabel;
    @UiField
    TextBox licenseName;
    @UiField
    ListBox licenseFeatures;
    @UiField
    DateBox expirationDate;
    @UiField
    TextBox licenseCountLimit;

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

        Admin.getService().getConfiguration(new AsyncCallback<Configuration>() {
            @Override
            public void onFailure(Throwable caught) {
                LOGGER.log(Level.SEVERE, caught.getMessage(), caught);
                initFeaturesListBox(FALLBACK_FEATURES);
            }

            @Override
            public void onSuccess(Configuration result) {
                initFeaturesListBox(result.getLicensePossibleFeatures());
            }
        });
    }

    private void initFeaturesListBox(List<String> features) {
        if (features != null) {
            for (String feature : features) {
                licenseFeatures.addItem(feature, feature);
            }
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
            licenseName.setValue(metadataAsObject.getLicenseName());
            licenseCountLimit.setValue(Integer.toString(metadataAsObject.getLicenseCountLimit()));
            expirationDate.setValue(metadataAsObject.getExpirationDate());

            // select license features
            for (int i = 0; i < licenseFeatures.getItemCount(); i++) {
                final String valueByIndex = licenseFeatures.getValue(i);
                if (metadataAsObject.getLicenseFeatures().contains(valueByIndex)) {
                    licenseFeatures.setItemSelected(i, true);
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
        final Integer licenseCountLimit = licenseCountLimit();
        final List<String> selectedLicenseFeatures = selectedLicenseFeatures();

        if ((numberOfLicenses == null || numberOfLicenses <= 0) && !isEditMode) {
            showError("Unable to parse number of licenses. Please enter integer more then zero.");
            return false;
        }
        if (licenseCountLimit == null || licenseCountLimit < 0) {
            showError("Unable to parse number of license count limit.");
            return false;
        }
        if (threadsCount == null || threadsCount < 0) {
            showError("Unable to parse number of threads.");
            return false;
        }
        if (selectedLicenseFeatures == null || selectedLicenseFeatures.isEmpty()) {
            showError("Please select any feature for license.");
            return false;
        }

        final String licenseName = this.licenseName.getValue();
        if (licenseName == null || licenseName.isEmpty()) {
            showError("Please enter name for license.");
            return false;
        }

        return true;
    }

    public LicenseMetadata licenseMetadata() {
        return new LicenseMetadata()
                .setLicenseFeatures(selectedLicenseFeatures())
                .setLicenseName(licenseName.getValue())
                .setMultiServer(multiServer.getValue())
                .setThreadsCount(threadsCount())
                .setLicenseCountLimit(licenseCountLimit())
                .setExpirationDate(expirationDate.getValue());
    }

    public List<String> selectedLicenseFeatures() {
        List<String> selectedItems = new ArrayList<String>();

        for (int i = 0; i < licenseFeatures.getItemCount(); i++) {
            if (licenseFeatures.isItemSelected(i)) {
                selectedItems.add(licenseFeatures.getValue(i));
            }
        }

        return selectedItems;
    }

    public Integer numberOfLicenses() {
        return Admin.parse(numberOfLicenseIds.getValue());
    }

    public Integer threadsCount() {
        return Admin.parse(threadsCount.getValue());
    }

    public Integer licenseCountLimit() {
        return Admin.parse(licenseCountLimit.getValue());
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
