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
    private List<String> jansServiceModule;

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

    public List<AssetDirMapping> getAssetDirMapping() {
        return assetDirMapping;
    }

    public void setAssetDirMapping(List<AssetDirMapping> assetDirMapping) {
        this.assetDirMapping = assetDirMapping;
    }

    public List<String> getJansServiceModule() {
        return jansServiceModule;
    }

    public void setJansServiceModule(List<String> jansServiceModule) {
        this.jansServiceModule = jansServiceModule;
    }

    @Override
    public String toString() {
        return "AssetMgtConfiguration [assetMgtEnabled=" + assetMgtEnabled + ", assetServerUploadEnabled="
                + assetServerUploadEnabled + ", fileExtensionValidationEnabled=" + fileExtensionValidationEnabled
                + ", moduleNameValidationEnabled=" + moduleNameValidationEnabled + ", assetBaseDirectory="
                + assetBaseDirectory + ", jansServiceModule=" + jansServiceModule + ", assetDirMapping=" + assetDirMapping + "]";
    }
}
