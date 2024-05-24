package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetMgtConfiguration {

    /**
     * Flag indicating if asset management functionality is enabled
     */
    private boolean assetMgtEnabled;

    /**
     * Flag indicating if asset upload to server is enabled
     */
    private boolean assetServerUploadEnabled;

    /**
     * Flag indicating if file extension validation is enabled
     */
    private boolean fileExtensionValidationEnabled;

    /**
     * Flag indicating if service module name extension validation is enabled
     */
    private boolean moduleNameValidationEnabled;

    /**
     * Base directory on server to upload the asset
     */
    private String assetBaseDirectory;

    /**
     * List of supported service module where asset can be uploaded
     */
    private List<String> jansModules;

    /**
     * Asset type mapped to server directory
     */
    private List<AssetDirMapping> assetDirMapping;

    public boolean isAssetMgtEnabled() {
        return assetMgtEnabled;
    }

    public void setAssetMgtEnabled(boolean assetMgtEnabled) {
        this.assetMgtEnabled = assetMgtEnabled;
    }

    public boolean isAssetServerUploadEnabled() {
        return assetServerUploadEnabled;
    }

    public void setAssetServerUploadEnabled(boolean assetServerUploadEnabled) {
        this.assetServerUploadEnabled = assetServerUploadEnabled;
    }

    public boolean isFileExtensionValidationEnabled() {
        return fileExtensionValidationEnabled;
    }

    public void setFileExtensionValidationEnabled(boolean fileExtensionValidationEnabled) {
        this.fileExtensionValidationEnabled = fileExtensionValidationEnabled;
    }

    public boolean isModuleNameValidationEnabled() {
        return moduleNameValidationEnabled;
    }

    public void setModuleNameValidationEnabled(boolean moduleNameValidationEnabled) {
        this.moduleNameValidationEnabled = moduleNameValidationEnabled;
    }

    public String getAssetBaseDirectory() {
        return assetBaseDirectory;
    }

    public void setAssetBaseDirectory(String assetBaseDirectory) {
        this.assetBaseDirectory = assetBaseDirectory;
    }

    public List<String> getJansModules() {
        return jansModules;
    }

    public void setJansModules(List<String> jansModules) {
        this.jansModules = jansModules;
    }

    public List<AssetDirMapping> getAssetDirMapping() {
        return assetDirMapping;
    }

    public void setAssetDirMapping(List<AssetDirMapping> assetDirMapping) {
        this.assetDirMapping = assetDirMapping;
    }

    @Override
    public String toString() {
        return "AssetMgtConfiguration [assetMgtEnabled=" + assetMgtEnabled + ", assetServerUploadEnabled="
                + assetServerUploadEnabled + ", fileExtensionValidationEnabled=" + fileExtensionValidationEnabled
                + ", moduleNameValidationEnabled=" + moduleNameValidationEnabled + ", assetBaseDirectory="
                + assetBaseDirectory + ", jansModules=" + jansModules + ", assetDirMapping=" + assetDirMapping + "]";
    }

}
