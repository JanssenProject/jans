package io.jans.as.model.config.adminui;

public class LicenseSpringCredentials {
    private String apiKey;
    private String productCode;
    private String sharedKey;
    private String managementKey;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getSharedKey() {
        return sharedKey;
    }

    public void setSharedKey(String sharedKey) {
        this.sharedKey = sharedKey;
    }

    public String getManagementKey() {
        return managementKey;
    }

    public void setManagementKey(String managementKey) {
        this.managementKey = managementKey;
    }

    @Override
    public String toString() {
        return "LicenseSpringCredentials{" +
                "apiKey='" + apiKey + '\'' +
                ", productCode='" + productCode + '\'' +
                ", sharedKey='" + sharedKey + '\'' +
                ", managementKey='" + managementKey + '\'' +
                '}';
    }
}
