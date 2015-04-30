package org.xdi.oxd.licenser.server;

import com.google.common.io.BaseEncoding;
import org.xdi.oxd.license.client.js.LdapLicenseCrypt;

import java.util.Date;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2014
 */

public class LicenseGeneratorInput {

    private Date expiredAt;
    private String customerName;
    private byte[] publicKey;
    private byte[] privateKey;
    private String privatePassword;
    private String publicPassword;
    private String licensePassword;
    private String metadata;

    public LicenseGeneratorInput setCrypt(LdapLicenseCrypt crypt) {
        setLicensePassword(crypt.getLicensePassword());
        setPrivatePassword(crypt.getPrivatePassword());
        setPublicPassword(crypt.getPublicPassword());
        setPrivateKey(BaseEncoding.base64().decode(crypt.getPrivateKey()));
        setPublicKey(BaseEncoding.base64().decode(crypt.getPublicKey()));
        return this;
    }

    public Date getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Date expiredAt) {
        this.expiredAt = expiredAt;
    }

    public String getCustomerName() {
        return customerName;
    }

    public void setCustomerName(String customerName) {
        this.customerName = customerName;
    }

    public byte[] getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(byte[] privateKey) {
        this.privateKey = privateKey;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public String getLicensePassword() {
        return licensePassword;
    }

    public void setLicensePassword(String licensePassword) {
        this.licensePassword = licensePassword;
    }

    public String getPrivatePassword() {
        return privatePassword;
    }

    public void setPrivatePassword(String privatePassword) {
        this.privatePassword = privatePassword;
    }

    public String getPublicPassword() {
        return publicPassword;
    }

    public void setPublicPassword(String publicPassword) {
        this.publicPassword = publicPassword;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }
}
