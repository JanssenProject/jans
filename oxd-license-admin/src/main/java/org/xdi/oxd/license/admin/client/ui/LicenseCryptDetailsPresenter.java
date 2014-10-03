package org.xdi.oxd.license.admin.client.ui;

import org.xdi.oxd.license.admin.client.Admin;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 25/09/2014
 */

public class LicenseCryptDetailsPresenter {

    private final LicenseCryptDetailsPanel view;

    private LdapLicenseCrypt licenseCrypt;

    public LicenseCryptDetailsPresenter(LicenseCryptDetailsPanel view) {
        this.view = view;
    }

    public void show(LdapLicenseCrypt licenseCrypt) {
        this.licenseCrypt = licenseCrypt;

        view.getNameField().setHTML(Admin.asHtml(licenseCrypt.getName()));
        view.getPrivateKey().setHTML(Admin.asHtml(licenseCrypt.getPrivateKey()));
        view.getPublicKey().setHTML(Admin.asHtml(licenseCrypt.getPublicKey()));
        view.getClientPublicKey().setHTML(Admin.asHtml(licenseCrypt.getClientPublicKey()));
        view.getClientPrivateKey().setHTML(Admin.asHtml(licenseCrypt.getClientPrivateKey()));
        view.getPrivatePassword().setHTML(Admin.asHtml(licenseCrypt.getPrivatePassword()));
        view.getPublicPassword().setHTML(Admin.asHtml(licenseCrypt.getPublicPassword()));
        view.getLicensePassword().setHTML(Admin.asHtml(licenseCrypt.getLicensePassword()));
    }

}
