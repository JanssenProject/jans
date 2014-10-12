package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.SelectionChangeEvent;
import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.admin.client.SuccessCallback;
import org.xdi.oxd.license.admin.client.dialogs.LicenseIdMetadataDialog;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;
import org.xdi.oxd.license.client.js.LdapLicenseId;
import org.xdi.oxd.license.client.js.LicenseMetadata;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 25/09/2014
 */

public class LicenseCryptDetailsPresenter {

    private final MultiSelectionModel<LdapLicenseId> selectionModel = new MultiSelectionModel<LdapLicenseId>();

    private final LicenseCryptDetailsPanel view;

    private LdapLicenseCrypt licenseCrypt;

    public LicenseCryptDetailsPresenter(LicenseCryptDetailsPanel view) {
        this.view = view;
        this.view.getGenerateLicenseIdButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                generateLicenseIds();
            }
        });
        this.view.getRemoveButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onRemove();
            }
        });
        this.view.getEditButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                onEdit();
            }
        });
        this.view.getRefreshButton().addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                loadLicenseIds();
            }
        });
        view.getLicenseIds().setSelectionModel(selectionModel);

        selectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
                setButtonsState();
            }
        });
        setButtonsState();
    }

    private void onRemove() {
        Admin.getService().remove(selectionModel.getSelectedSet(), new SuccessCallback<Void>() {
            @Override
            public void onSuccess(Void result) {
                LicenseCryptDetailsPresenter.this.loadLicenseIds();
            }
        });
    }

    private void onEdit() {
        LicenseIdMetadataDialog dialog = new LicenseIdMetadataDialog(selectionModel.getSelectedSet().iterator().next()) {
            @Override
            public void onOk() {
                Admin.getService().save(getLicenseId(), new SuccessCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        loadLicenseIds();
                    }
                });
            }
        };
        dialog.show();
    }

    private void setButtonsState() {
        this.view.getRemoveButton().setEnabled(!selectionModel.getSelectedSet().isEmpty());
        this.view.getEditButton().setEnabled(selectionModel.getSelectedSet().size() == 1);
    }

    public void show(LdapLicenseCrypt licenseCrypt) {
        this.licenseCrypt = licenseCrypt;
        this.view.getGenerateLicenseIdButton().setEnabled(licenseCrypt != null);
        if (licenseCrypt == null) {
            clear();
            return;
        }

        loadLicenseIds();

        view.getNameField().setHTML(Admin.asHtml(licenseCrypt.getName()));
        view.getPrivateKey().setHTML(Admin.asHtml(licenseCrypt.getPrivateKey()));
        view.getPublicKey().setHTML(Admin.asFullHtml(licenseCrypt.getPublicKey()));
        view.getClientPublicKey().setHTML(Admin.asHtml(licenseCrypt.getClientPublicKey()));
        view.getClientPrivateKey().setHTML(Admin.asHtml(licenseCrypt.getClientPrivateKey()));
        view.getPrivatePassword().setHTML(Admin.asHtml(licenseCrypt.getPrivatePassword()));
        view.getPublicPassword().setHTML(Admin.asHtml(licenseCrypt.getPublicPassword()));
        view.getLicensePassword().setHTML(Admin.asHtml(licenseCrypt.getLicensePassword()));
        view.getLicenseIdCount().setHTML("0");
    }

    public void clear() {
        view.getNameField().setHTML("");
        view.getPrivateKey().setHTML("");
        view.getPublicKey().setHTML("");
        view.getClientPublicKey().setHTML("");
        view.getClientPrivateKey().setHTML("");
        view.getPrivatePassword().setHTML("");
        view.getPublicPassword().setHTML("");
        view.getLicensePassword().setHTML("");
        view.getLicenseIdCount().setHTML("0");
        view.getLicenseIds().setRowCount(0);
        view.getLicenseIds().setRowData(new ArrayList<LdapLicenseId>());
    }


    private void generateLicenseIds() {
        LicenseIdMetadataDialog inputNumberDialog = new LicenseIdMetadataDialog(null) {
            @Override
            public void onOk() {
                generateLicenseIdsImpl(numberOfLicenses(), licenseMetadata());
            }
        };
        inputNumberDialog.show();
    }

    private void generateLicenseIdsImpl(int licenseIdsCount, LicenseMetadata metadata) {
        Admin.getService().generateLicenseIds(licenseIdsCount, licenseCrypt, metadata, new SuccessCallback<List<LdapLicenseId>>() {
            @Override
            public void onSuccess(List<LdapLicenseId> result) {
                loadLicenseIds();
            }
        });
    }

    private void loadLicenseIds() {

        if (licenseCrypt == null) {
            return;
        }

        Admin.getService().loadLicenseIdsByCrypt(licenseCrypt, new SuccessCallback<List<LdapLicenseId>>() {
            @Override
            public void onSuccess(List<LdapLicenseId> result) {
                view.getLicenseIdCount().setHTML(Integer.toString(result.size()));
                view.getLicenseIds().setRowCount(result.size());
                view.getLicenseIds().setRowData(result);
            }
        });
    }


}
