package io.jans.configapi.model.configuration;

import org.apache.commons.lang.StringUtils;

public class CorsConfigurationFilter {

    private String filterName;
    private Boolean corsEnabled;
    private String corsAllowedOrigins;
    private String corsAllowedMethods;
    private String corsAllowedHeaders;
    private String corsExposedHeaders;
    private Boolean corsSupportCredentials;
    private Boolean corsLoggingEnabled;
    private Integer corsPreflightMaxAge;
    private Boolean corsRequestDecorate;

    public static final Boolean DEFAULT_CORS_ENABLED = true;
    public static final String DEFAULT_CORS_ALLOWED_ORIGINS = "*";
    public static final String DEFAULT_CORS_ALLOWED_METHODS = "GET,POST,HEAD,OPTIONS";
    public static final String DEFAULT_CORS_ALLOWED_HEADERS = "";
    public static final String DEFAULT_CORS_EXPOSED_HEADERS = "";
    public static final Boolean DEFAULT_CORS_SUPPORT_CREDENTIALS = true;
    public static final Boolean DEFAULT_CORS_LOGGING_ENABLED = false;
    public static final Integer DEFAULT_CORS_PREFLIGHT_MAX_AGE = 1800;
    public static final Boolean DEFAULT_CORS_REQUEST_DECORATE = true;

    public String getFilterName() {
        return filterName;
    }

    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }

    public Boolean getCorsEnabled() {
        if (corsEnabled == null) {
            corsEnabled = DEFAULT_CORS_ENABLED;
        }
        return corsEnabled;
    }

    public void setCorsEnabled(Boolean corsEnabled) {
        this.corsEnabled = corsEnabled;
    }

    public String getCorsAllowedOrigins() {
        if (StringUtils.isEmpty(corsAllowedOrigins)) {
            corsAllowedOrigins = DEFAULT_CORS_ALLOWED_ORIGINS;
        }
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(String corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public String getCorsAllowedMethods() {
        if (StringUtils.isEmpty(corsAllowedMethods)) {
            corsAllowedMethods = DEFAULT_CORS_ALLOWED_METHODS;
        }
        return corsAllowedMethods;
    }

    public void setCorsAllowedMethods(String corsAllowedMethods) {
        this.corsAllowedMethods = corsAllowedMethods;
    }

    public String getCorsAllowedHeaders() {
        if (StringUtils.isEmpty(corsAllowedHeaders)) {
            corsAllowedHeaders = DEFAULT_CORS_ALLOWED_HEADERS;
        }
        return corsAllowedHeaders;
    }

    public void setCorsAllowedHeaders(String corsAllowedHeaders) {
        this.corsAllowedHeaders = corsAllowedHeaders;
    }

    public String getCorsExposedHeaders() {
        if (StringUtils.isEmpty(corsExposedHeaders)) {
            corsExposedHeaders = DEFAULT_CORS_EXPOSED_HEADERS;
        }
        return corsExposedHeaders;
    }

    public void setCorsExposedHeaders(String corsExposedHeaders) {
        this.corsExposedHeaders = corsExposedHeaders;
    }

    public Boolean getCorsSupportCredentials() {
        if (corsSupportCredentials == null) {
            corsSupportCredentials = DEFAULT_CORS_SUPPORT_CREDENTIALS;
        }

        return corsSupportCredentials;
    }

    public void setCorsSupportCredentials(Boolean corsSupportCredentials) {
        this.corsSupportCredentials = corsSupportCredentials;
    }

    public Boolean getCorsLoggingEnabled() {
        if (corsLoggingEnabled == null) {
            corsLoggingEnabled = DEFAULT_CORS_LOGGING_ENABLED;
        }
        return corsLoggingEnabled;
    }

    public void setCorsLoggingEnabled(Boolean corsLoggingEnabled) {
        this.corsLoggingEnabled = corsLoggingEnabled;
    }

    public Integer getCorsPreflightMaxAge() {
        if (corsPreflightMaxAge == null) {
            corsPreflightMaxAge = DEFAULT_CORS_PREFLIGHT_MAX_AGE;
        }
        return corsPreflightMaxAge;
    }

    public void setCorsPreflightMaxAge(Integer corsPreflightMaxAge) {
        this.corsPreflightMaxAge = corsPreflightMaxAge;
    }

    public Boolean getCorsRequestDecorate() {
        if (corsRequestDecorate == null) {
            corsRequestDecorate = DEFAULT_CORS_REQUEST_DECORATE;
        }
        return corsRequestDecorate;
    }

    public void setCorsRequestDecorate(Boolean corsRequestDecorate) {
        this.corsRequestDecorate = corsRequestDecorate;
    }

    @Override
    public String toString() {
        return "CorsConfigurationFilter [filterName=" + filterName + ", corsEnabled=" + corsEnabled
                + ", corsAllowedOrigins=" + corsAllowedOrigins + ", corsAllowedMethods=" + corsAllowedMethods
                + ", corsAllowedHeaders=" + corsAllowedHeaders + ", corsExposedHeaders=" + corsExposedHeaders
                + ", corsSupportCredentials=" + corsSupportCredentials + ", corsLoggingEnabled=" + corsLoggingEnabled
                + ", corsPreflightMaxAge=" + corsPreflightMaxAge + ", corsRequestDecorate=" + corsRequestDecorate + "]";
    }

}