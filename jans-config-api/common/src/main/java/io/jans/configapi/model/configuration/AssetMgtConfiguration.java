package io.jans.configapi.model.configuration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;
import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AssetMgtConfiguration implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    @Schema(description = "Flag indicating if asset management functionality is enabled.")
    private boolean assetMgtEnabled;

    @Schema(description = "Flag indicating if asset upload to server is enabled.")
    private boolean assetServerUploadEnabled;

    @Schema(description = "Flag indicating if file extension validation is enabled.")
    private boolean fileExtensionValidationEnabled;

    @Schema(description = "Flag indicating if service module name extension validation is enabled.")
    private boolean moduleNameValidationEnabled;

    @Schema(description = "List of supported service module where asset can be uploaded.")
    private List<String> jansServiceModule;

    @Schema(description = "Asset type mapped to server directory.")
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
                + ", moduleNameValidationEnabled=" + moduleNameValidationEnabled + ", jansServiceModule="
                + jansServiceModule + ", assetDirMapping=" + assetDirMapping + "]";
    }
}
