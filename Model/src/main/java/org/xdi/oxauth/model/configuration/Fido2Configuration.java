package org.xdi.oxauth.model.configuration;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * FIDO 2 configuration
 *
 * @author Yuriy Movchan Date: 11/05/2018
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fido2Configuration {

    private String certFilesFolder;

    private String mdsTocRootCertFile;
    private String mdsTocFilesFolder;

    private boolean userAutoEnrollment = false;

    private int unfinishedRequestExpiration = 120; // 120 seconds
    private int authenticationHistoryExpiration = 15 * 24 * 3600; // 15 days

    private String certificationServerMetadataFolder;

    private boolean disable;

    public String getCertificationServerMetadataFolder() {
        return certificationServerMetadataFolder;
    }

    public void setCertificationServerMetadataFolder(String certificationServerMetadataFolder) {
        this.certificationServerMetadataFolder = certificationServerMetadataFolder;
    }

    public String getCertFilesFolder() {
        return certFilesFolder;
    }

    public void setCertFilesFolder(String certFilesFolder) {
        this.certFilesFolder = certFilesFolder;
    }

    public String getMdsTocRootCertFile() {
        return mdsTocRootCertFile;
    }

    public void setMdsTocRootCertFile(String mdsTocRootCertFile) {
        this.mdsTocRootCertFile = mdsTocRootCertFile;
    }

    public String getMdsTocFilesFolder() {
        return mdsTocFilesFolder;
    }

    public void setMdsTocFilesFolder(String mdsTocFilesFolder) {
        this.mdsTocFilesFolder = mdsTocFilesFolder;
    }

    public boolean isUserAutoEnrollment() {
        return userAutoEnrollment;
    }

    public void setUserAutoEnrollment(boolean userAutoEnrollment) {
        this.userAutoEnrollment = userAutoEnrollment;
    }

    public int getUnfinishedRequestExpiration() {
        return unfinishedRequestExpiration;
    }

    public void setUnfinishedRequestExpiration(int unfinishedRequestExpiration) {
        this.unfinishedRequestExpiration = unfinishedRequestExpiration;
    }

    public int getAuthenticationHistoryExpiration() {
        return authenticationHistoryExpiration;
    }

    public void setAuthenticationHistoryExpiration(int authenticationHistoryExpiration) {
        this.authenticationHistoryExpiration = authenticationHistoryExpiration;
    }

    public boolean isDisable() {
        return disable;
    }

    public void setDisable(boolean disable) {
        this.disable = disable;
    }

}
