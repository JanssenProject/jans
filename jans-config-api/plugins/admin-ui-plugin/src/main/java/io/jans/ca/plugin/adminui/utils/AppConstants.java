package io.jans.ca.plugin.adminui.utils;

public interface AppConstants {
    public static final String ADMIN_UI_CONFIG_DN = "ou=admin-ui,ou=configuration,o=jans";
    public static final String ADS_CONFIG_DN = "ou=agama-developer-studio,ou=configuration,o=jans";
    public static final String LICENSE_SPRING_API_URL = "https://api.licensespring.com/api/v4/";
    //application type
    public static final String APPLICATION_KEY_ADMIN_UI = "admin-ui";
    public static final String APPLICATION_KEY_ADS = "ads";
    public static final String SCOPE_ADMINUI_READ = "https://jans.io/oauth/jans-auth-server/config/adminui/read-all";
    public static final String SCOPE_ADMINUI_WRITE = "https://jans.io/oauth/jans-auth-server/config/adminui/write-all";
    public static final String SCOPE_ADMINUI_DELETE = "https://jans.io/oauth/jans-auth-server/config/adminui/delete-all";
}
