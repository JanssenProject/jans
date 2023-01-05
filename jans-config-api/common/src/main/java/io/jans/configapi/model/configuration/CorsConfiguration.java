package io.jans.configapi.model.configuration;

import java.util.*;
import java.util.Optional;
import jakarta.servlet.*;

public class CorsConfiguration {

    /**
     * A {@link Collection} of origins consisting of zero or more origins that are
     * allowed access to the resource.
     */
    private Collection<String> allowedOrigins;

    /**
     * A {@link Collection} of methods consisting of zero or more methods that are
     * supported by the resource.
     */
    private final Collection<String> allowedHttpMethods;

    /**
     * A {@link Collection} of headers consisting of zero or more header field names
     * that are supported by the resource.
     */
    private final Collection<String> allowedHttpHeaders;

    /**
     * A {@link Collection} of exposed headers consisting of zero or more header
     * field names of headers other than the simple response headers that the
     * resource might use and can be exposed.
     */
    private final Collection<String> exposedHeaders;

    /**
     * A supports credentials flag that indicates whether the resource supports user
     * credentials in the request. It is true when the resource does and false
     * otherwise.
     */
    private boolean supportsCredentials;

    /**
     * Indicates (in seconds) how long the results of a pre-flight request can be
     * cached in a pre-flight result cache.
     */
    private long preflightMaxAge;

    /**
     * Determines if the request should be decorated or not.
     */
    private boolean decorateRequest;
    private boolean enabled;

    public CorsConfiguration() {
        this.allowedOrigins = new HashSet<String>();
        this.allowedHttpMethods = new HashSet<String>();
        this.allowedHttpHeaders = new HashSet<String>();
        this.exposedHeaders = new HashSet<String>();
    }

    public Collection<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    public void setAllowedOrigins(Collection<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    public boolean isSupportsCredentials() {
        return supportsCredentials;
    }

    public void setSupportsCredentials(boolean supportsCredentials) {
        this.supportsCredentials = supportsCredentials;
    }

    public long getPreflightMaxAge() {
        return preflightMaxAge;
    }

    public void setPreflightMaxAge(long preflightMaxAge) {
        this.preflightMaxAge = preflightMaxAge;
    }

    public boolean isDecorateRequest() {
        return decorateRequest;
    }

    public void setDecorateRequest(boolean decorateRequest) {
        this.decorateRequest = decorateRequest;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Collection<String> getAllowedHttpMethods() {
        return allowedHttpMethods;
    }

    public Collection<String> getAllowedHttpHeaders() {
        return allowedHttpHeaders;
    }

    public Collection<String> getExposedHeaders() {
        return exposedHeaders;
    }

    /**
     * Parses each param-value and populates configuration variables. If a param is
     * provided, it overrides the default.
     *
     * @param corsEnabled         "true" if CORS needs to be enabled.
     * @param allowedOrigins      A {@link String} of comma separated origins.
     * @param allowedHttpMethods  A {@link String} of comma separated HTTP methods.
     * @param allowedHttpHeaders  A {@link String} of comma separated HTTP headers.
     * @param exposedHeaders      A {@link String} of comma separated headers that
     *                            needs to be exposed.
     * @param supportsCredentials "true" if support credentials needs to be enabled.
     * @param preflightMaxAge     The amount of seconds the user agent is allowed to
     *                            cache the result of the pre-flight request.
     * @param decorateRequest     "true" if request needs to enhanced
     * @throws ServletException
     */
    public void parseAndStore(final String corsEnabled, final String allowedOrigins, final String allowedHttpMethods,
            final String allowedHttpHeaders, final String exposedHeaders, final String supportsCredentials,
            final String preflightMaxAge, final String decorateRequest) throws ServletException {

        this.enabled = Boolean.parseBoolean(corsEnabled);

        if (allowedOrigins != null) {
            if (!allowedOrigins.trim().equals("*")) {
                Set<String> setAllowedOrigins = parseStringToSet(allowedOrigins);
                this.allowedOrigins.clear();
                this.allowedOrigins.addAll(setAllowedOrigins);
            }
        }

        if (allowedHttpMethods != null) {
            Set<String> setAllowedHttpMethods = parseStringToSet(allowedHttpMethods);
            this.allowedHttpMethods.clear();
            this.allowedHttpMethods.addAll(setAllowedHttpMethods);
        }

        if (allowedHttpHeaders != null) {
            Set<String> setAllowedHttpHeaders = parseStringToSet(allowedHttpHeaders);
            Set<String> lowerCaseHeaders = new HashSet<String>();
            for (String header : setAllowedHttpHeaders) {
                String lowerCase = header.toLowerCase();
                lowerCaseHeaders.add(lowerCase);
            }
            this.allowedHttpHeaders.clear();
            this.allowedHttpHeaders.addAll(lowerCaseHeaders);
        }

        if (exposedHeaders != null) {
            Set<String> setExposedHeaders = parseStringToSet(exposedHeaders);
            this.exposedHeaders.clear();
            this.exposedHeaders.addAll(setExposedHeaders);
        }

        if (supportsCredentials != null) {
            // For any value other then 'true' this will be false.
            this.supportsCredentials = Boolean.parseBoolean(supportsCredentials);
        }

        if (preflightMaxAge != null) {
            try {
                if (!preflightMaxAge.isEmpty()) {
                    this.preflightMaxAge = Long.parseLong(preflightMaxAge);
                } else {
                    this.preflightMaxAge = 0L;
                }
            } catch (NumberFormatException e) {
                throw new ServletException("corsFilter.invalidPreflightMaxAge", e);
            }
        }

        if (decorateRequest != null) {
            // For any value other then 'true' this will be false.
            this.decorateRequest = Boolean.parseBoolean(decorateRequest);
        }
    }

    /**
     * Takes a comma separated list and returns a Set<String>.
     *
     * @param data A comma separated list of strings.
     * @return Set<String>
     */
    private Set<String> parseStringToSet(final String data) {
        String[] splits;

        if (data != null && data.length() > 0) {
            splits = data.split(",");
        } else {
            splits = new String[] {};
        }

        Set<String> set = new HashSet<String>();
        if (splits.length > 0) {
            for (String split : splits) {
                set.add(split.trim());
            }
        }

        return set;
    }

    public boolean isOriginAllowed(final String origin) {
        if (isAnyOriginAllowed()) {
            return true;
        }

        // If 'Origin' header is a case-sensitive match of any of allowed
        // origins, then return true, else return false.
        return allowedOrigins.contains(origin);
    }

    public boolean isAnyOriginAllowed() {
        if (allowedOrigins != null && allowedOrigins.size() == 0) {
            return true;
        }
        return false;
    }

    @Override
    public String toString() {
        return "CorsConfiguration [enabled=" + enabled + " ,allowedOrigins=" + allowedOrigins + ", allowedHttpMethods="
                + allowedHttpMethods + ", allowedHttpHeaders=" + allowedHttpHeaders + ", exposedHeaders="
                + exposedHeaders + ", supportsCredentials=" + supportsCredentials + ", preflightMaxAge="
                + preflightMaxAge + ", decorateRequest=" + decorateRequest + "]";
    }

}
