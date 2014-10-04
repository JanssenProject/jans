package org.xdi.oxd.license.admin.client.ui;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.admin.client.SuccessCallback;
import org.xdi.oxd.license.admin.client.dialogs.InputNumberDialog;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;
import org.xdi.oxd.license.client.js.LdapLicenseId;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 25/09/2014
 */

public class LicenseCryptDetailsPresenter {

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
    }

    public void show(LdapLicenseCrypt licenseCrypt) {
        this.licenseCrypt = licenseCrypt;
        if (licenseCrypt == null) {
            clear();
            return;
        }

        loadLicenseIds();

        view.getNameField().setHTML(Admin.asHtml(licenseCrypt.getName()));
        view.getPrivateKey().setHTML(Admin.asHtml(licenseCrypt.getPrivateKey()));
        view.getPublicKey().setHTML(Admin.asHtml(licenseCrypt.getPublicKey()));
        view.getClientPublicKey().setHTML(Admin.asHtml(licenseCrypt.getClientPublicKey()));
        view.getClientPrivateKey().setHTML(Admin.asHtml(licenseCrypt.getClientPrivateKey()));
        view.getPrivatePassword().setHTML(Admin.asHtml(licenseCrypt.getPrivatePassword()));
        view.getPublicPassword().setHTML(Admin.asHtml(licenseCrypt.getPublicPassword()));
        view.getLicensePassword().setHTML(Admin.asHtml(licenseCrypt.getLicensePassword()));
        view.getLicenseIdCount().setHTML("0");
    }

    private void clear() {
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
        InputNumberDialog inputNumberDialog = new InputNumberDialog() {
            @Override
            public void onOk() {
                generateLicenseIdsImpl(numberOfLicenses());
            }
        };
        inputNumberDialog.show();
    }

    private void generateLicenseIdsImpl(int licenseIdsCount) {
        Admin.getService().generateLicenseIds(licenseIdsCount, licenseCrypt, new SuccessCallback<List<LdapLicenseId>>() {
            @Override
            public void onSuccess(List<LdapLicenseId> result) {
                loadLicenseIds();
            }
        });
    }

    private void loadLicenseIds() {
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
