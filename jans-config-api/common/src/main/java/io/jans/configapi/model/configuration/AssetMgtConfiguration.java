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
     * Base directory on server to upload the asset
     */
    private String assetBaseDirectory;

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

    @Override
    public String toString() {
        return "AssetMgtConfiguration [assetMgtEnabled=" + assetMgtEnabled + ", assetServerUploadEnabled="
                + assetServerUploadEnabled + ", assetBaseDirectory=" + assetBaseDirectory + ", assetDirMapping="
                + assetDirMapping + "]";
    }

}
