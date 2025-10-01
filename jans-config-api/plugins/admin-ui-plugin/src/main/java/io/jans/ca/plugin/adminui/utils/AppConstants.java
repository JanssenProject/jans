package io.jans.ca.plugin.adminui.utils;

public interface AppConstants {
    public static final String ADMIN_UI_CONFIG_DN = "ou=admin-ui,ou=configuration,o=jans";
    public static final String WEBHOOK_DN = "ou=auiWebhooks,ou=admin-ui,o=jans";
    public static final String ADMIN_UI_FEATURES_DN = "ou=auiFeatures,ou=admin-ui,o=jans";
    public static final String ADS_CONFIG_DN = "ou=agama-developer-studio,ou=configuration,o=jans";
    public static final String LICENSE_SPRING_API_URL = "https://api.licensespring.com/api/v4/";
    //application type
    public static final String APPLICATION_KEY_ADMIN_UI = "admin-ui";
    public static final String APPLICATION_KEY_ADS = "ads";
    public static final String SCOPE_ADMINUI_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/read-all";
    public static final String SCOPE_ADMINUI_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/write-all";
    public static final String SCOPE_ADMINUI_DELETE = "https://jans.io/oauth/jans-auth-server/config/adminui/delete-all";
    //scan api urls
    public static final String SCAN_DEV_AUTH_SERVER = "https://account-dev.gluu.cloud";
    public static final String SCAN_PROD_AUTH_SERVER = "https://account.gluu.org";
    public static final String SCAN_DEV_SERVER = "https://cloud-dev.gluu.cloud";
    public static final String SCAN_PROD_SERVER = "https://cloud.gluu.org";
    //fields name
    public static final String WEBHOOK_ID = "webhookId";
    public static final String INUM = "inum";
    public static final String ADMIN_UI_FEATURE_ID = "featureId";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String APPLICATION_JSON = "application/json";

    public static final int LICENSE_DETAILS_SYNC_INTERVAL_IN_DAYS = 30;

}
