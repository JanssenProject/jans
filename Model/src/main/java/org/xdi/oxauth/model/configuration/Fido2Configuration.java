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

    private String mdsTocRootFileLocation;
    private String mdsTocFilesFolder;

    private String certificationServerMetadataFolder;

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

    public String getMdsTocRootFileLocation() {
        return mdsTocRootFileLocation;
    }

    public void setMdsTocRootFileLocation(String mdsTocRootFileLocation) {
        this.mdsTocRootFileLocation = mdsTocRootFileLocation;
    }

    public String getMdsTocFilesFolder() {
        return mdsTocFilesFolder;
    }

    public void setMdsTocFilesFolder(String mdsTocFilesFolder) {
        this.mdsTocFilesFolder = mdsTocFilesFolder;
    }

}
