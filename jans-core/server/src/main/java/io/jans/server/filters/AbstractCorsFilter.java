/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.server.filters;

import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.MessageFormat;
import java.util.*;

/**
 * CORS Filter to support both Tomcat and Jetty
 *
 * @author Yuriy Movchan
 * @author Javier Rojas Blum
 * @version March 22, 2018
 */
public abstract class AbstractCorsFilter implements Filter {

    @Inject
    private Logger log;

    private static final StringManager SM =
            StringManager.getManager(Constants.PACKAGE);

    /**
     * A {@link Collection} of origins consisting of zero or more origins that
     * are allowed access to the resource.
     */
    private Collection<String> allowedOrigins;

    /**
     * A {@link Collection} of methods consisting of zero or more methods that
     * are supported by the resource.
     */
    private final Collection<String> allowedHttpMethods;

    /**
     * A {@link Collection} of headers consisting of zero or more header field
     * names that are supported by the resource.
     */
    private final Collection<String> allowedHttpHeaders;

    /**
     * A {@link Collection} of exposed headers consisting of zero or more header
     * field names of headers other than the simple response headers that the
     * resource might use and can be exposed.
     */
    private final Collection<String> exposedHeaders;

    /**
     * A supports credentials flag that indicates whether the resource supports
     * user credentials in the request. It is true when the resource does and
     * false otherwise.
     */
    private boolean supportsCredentials;

    /**
     * Indicates (in seconds) how long the results of a pre-flight request can
     * be cached in a pre-flight result cache.
     */
    private long preflightMaxAge;

    /**
     * Determines if the request should be decorated or not.
     */
    private boolean decorateRequest;

    public AbstractCorsFilter() {
        this.allowedOrigins = new HashSet<String>();
        this.allowedHttpMethods = new HashSet<String>();
        this.allowedHttpHeaders = new HashSet<String>();
        this.exposedHeaders = new HashSet<String>();
    }

    @Override
    public void doFilter(final ServletRequest servletRequest,
                         final ServletResponse servletResponse, final FilterChain filterChain)
            throws IOException, ServletException {
        if (!(servletRequest instanceof HttpServletRequest)
                || !(servletResponse instanceof HttpServletResponse)) {
            throw new ServletException(SM.getString("corsFilter.onlyHttp"));
        }

        // Safe to downcast at this point.
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Determines the CORS request type.
        AbstractCorsFilter.CORSRequestType requestType = checkRequestType(request);

        // Adds CORS specific attributes to request.
        if (decorateRequest) {
            AbstractCorsFilter.decorateCORSProperties(request, requestType);
        }
        switch (requestType) {
            case SIMPLE:
                // Handles a Simple CORS request.
                this.handleSimpleCORS(request, response, filterChain);
                break;
            case ACTUAL:
                // Handles an Actual CORS request.
                this.handleSimpleCORS(request, response, filterChain);
                break;
            case PRE_FLIGHT:
                // Handles a Pre-flight CORS request.
                this.handlePreflightCORS(request, response, filterChain);
                break;
            case NOT_CORS:
                // Handles a Normal request that is not a cross-origin request.
                this.handleNonCORS(request, response, filterChain);
                break;
            default:
                // Handles a CORS request that violates specification.
                this.handleInvalidCORS(request, response, filterChain);
                break;
        }
    }

    @Override
    public abstract void init(final FilterConfig filterConfig) throws ServletException;


    /**
     * Handles a CORS request of type {@link CORSRequestType}.SIMPLE.
     *
     * @param request     The {@link HttpServletRequest} object.
     * @param response    The {@link HttpServletResponse} object.
     * @param filterChain The {@link FilterChain} object.
     * @throws IOException
     * @throws ServletException
     * @see <a href="http://www.w3.org/TR/cors/#resource-requests">Simple
     * Cross-Origin Request, Actual Request, and Redirects</a>
     */
    protected void handleSimpleCORS(final HttpServletRequest request,
                                    final HttpServletResponse response, final FilterChain filterChain)
            throws IOException, ServletException {

        AbstractCorsFilter.CORSRequestType requestType = checkRequestType(request);
        if (!(requestType == AbstractCorsFilter.CORSRequestType.SIMPLE
                || requestType == AbstractCorsFilter.CORSRequestType.ACTUAL)) {
            throw new IllegalArgumentException(
                    SM.getString("corsFilter.wrongType2",
                            AbstractCorsFilter.CORSRequestType.SIMPLE,
                            AbstractCorsFilter.CORSRequestType.ACTUAL));
        }

        final String origin = request
                .getHeader(AbstractCorsFilter.REQUEST_HEADER_ORIGIN);
        final String method = request.getMethod();

        // Section 6.1.2
        if (!isOriginAllowed(origin)) {
            handleInvalidCORS(request, response, filterChain);
            return;
        }

        if (!allowedHttpMethods.contains(method)) {
            handleInvalidCORS(request, response, filterChain);
            return;
        }

        // Section 6.1.3
        // Add a single Access-Control-Allow-Origin header.
        if (isAnyOriginAllowed() && !supportsCredentials) {
            // If resource doesn't support credentials and if any origin is
            // allowed
            // to make CORS request, return header with '*'.
            response.addHeader(
                    AbstractCorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN,
                    "*");
        } else {
            // If the resource supports credentials add a single
            // Access-Control-Allow-Origin header, with the value of the Origin
            // header as value.
            response.addHeader(
                    AbstractCorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN,
                    origin);
        }

        // Section 6.1.3
        // If the resource supports credentials, add a single
        // Access-Control-Allow-Credentials header with the case-sensitive
        // string "true" as value.
        if (supportsCredentials) {
            response.addHeader(
                    AbstractCorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS,
                    "true");
        }

        // Section 6.1.4
        // If the list of exposed headers is not empty add one or more
        // Access-Control-Expose-Headers headers, with as values the header
        // field names given in the list of exposed headers.
        if ((exposedHeaders != null) && (exposedHeaders.size() > 0)) {
            String exposedHeadersString = join(exposedHeaders, ",");
            response.addHeader(
                    AbstractCorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS,
                    exposedHeadersString);
        }

        // Forward the request down the filter chain.
        filterChain.doFilter(request, response);
    }


    /**
     * Handles CORS pre-flight request.
     *
     * @param request     The {@link HttpServletRequest} object.
     * @param response    The {@link HttpServletResponse} object.
     * @param filterChain The {@link FilterChain} object.
     * @throws IOException
     * @throws ServletException
     */
    protected void handlePreflightCORS(final HttpServletRequest request,
                                       final HttpServletResponse response, final FilterChain filterChain)
            throws IOException, ServletException {

        CORSRequestType requestType = checkRequestType(request);
        if (requestType != CORSRequestType.PRE_FLIGHT) {
            throw new IllegalArgumentException(
                    SM.getString("corsFilter.wrongType1",
                            CORSRequestType.PRE_FLIGHT.name().toLowerCase()));
        }

        final String origin = request
                .getHeader(AbstractCorsFilter.REQUEST_HEADER_ORIGIN);

        // Section 6.2.2
        if (!isOriginAllowed(origin)) {
            handleInvalidCORS(request, response, filterChain);
            return;
        }

        // Section 6.2.3
        String accessControlRequestMethod = request.getHeader(
                AbstractCorsFilter.REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
        if (accessControlRequestMethod == null
                || !HTTP_METHODS.contains(accessControlRequestMethod.trim())) {
            handleInvalidCORS(request, response, filterChain);
            return;
        } else {
            accessControlRequestMethod = accessControlRequestMethod.trim();
        }

        // Section 6.2.4
        String accessControlRequestHeadersHeader = request.getHeader(
                AbstractCorsFilter.REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
        List<String> accessControlRequestHeaders = new LinkedList<String>();
        if (accessControlRequestHeadersHeader != null
                && !accessControlRequestHeadersHeader.trim().isEmpty()) {
            String[] headers = accessControlRequestHeadersHeader.trim().split(
                    ",");
            for (String header : headers) {
                accessControlRequestHeaders.add(header.trim().toLowerCase());
            }
        }

        // Section 6.2.5
        if (!allowedHttpMethods.contains(accessControlRequestMethod)) {
            handleInvalidCORS(request, response, filterChain);
            return;
        }

        // Section 6.2.6
        if (!accessControlRequestHeaders.isEmpty()) {
            for (String header : accessControlRequestHeaders) {
                if (!allowedHttpHeaders.contains(header)) {
                    handleInvalidCORS(request, response, filterChain);
                    return;
                }
            }
        }

        // Section 6.2.7
        if (supportsCredentials) {
            response.addHeader(
                    AbstractCorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN,
                    origin);
            response.addHeader(
                    AbstractCorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS,
                    "true");
        } else {
            if (isAnyOriginAllowed()) {
                response.addHeader(
                        AbstractCorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN,
                        "*");
            } else {
                response.addHeader(
                        AbstractCorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN,
                        origin);
            }
        }

        // Section 6.2.8
        if (preflightMaxAge > 0) {
            response.addHeader(
                    AbstractCorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_MAX_AGE,
                    String.valueOf(preflightMaxAge));
        }

        // Section 6.2.9
        response.addHeader(
                AbstractCorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS,
                accessControlRequestMethod);

        // Section 6.2.10
        if ((allowedHttpHeaders != null) && (!allowedHttpHeaders.isEmpty())) {
            response.addHeader(
                    AbstractCorsFilter.RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS,
                    join(allowedHttpHeaders, ","));
        }

        // Do not forward the request down the filter chain.
    }


    /**
     * Handles a request, that's not a CORS request, but is a valid request i.e.
     * it is not a cross-origin request. This implementation, just forwards the
     * request down the filter chain.
     *
     * @param request     The {@link HttpServletRequest} object.
     * @param response    The {@link HttpServletResponse} object.
     * @param filterChain The {@link FilterChain} object.
     * @throws IOException
     * @throws ServletException
     */
    private void handleNonCORS(final HttpServletRequest request,
                               final HttpServletResponse response, final FilterChain filterChain)
            throws IOException, ServletException {
        // Let request pass.
        filterChain.doFilter(request, response);
    }


    /**
     * Handles a CORS request that violates specification.
     *
     * @param request     The {@link HttpServletRequest} object.
     * @param response    The {@link HttpServletResponse} object.
     * @param filterChain The {@link FilterChain} object.
     */
    private void handleInvalidCORS(final HttpServletRequest request,
                                   final HttpServletResponse response, final FilterChain filterChain) {
        String origin = request.getHeader(AbstractCorsFilter.REQUEST_HEADER_ORIGIN);
        String method = request.getMethod();
        String accessControlRequestHeaders = request.getHeader(
                REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);

        response.setContentType("text/plain");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.resetBuffer();

        if (log.isDebugEnabled()) {
            // Debug so no need for i18n
            StringBuilder message =
                    new StringBuilder("Invalid CORS request; Origin=");
            message.append(origin);
            message.append(";Method=");
            message.append(method);
            if (accessControlRequestHeaders != null) {
                message.append(";Access-Control-Request-Headers=");
                message.append(accessControlRequestHeaders);
            }
            log.debug(message.toString());
        }
    }


    @Override
    public void destroy() {
        // NOOP
    }


    /**
     * Decorates the {@link HttpServletRequest}, with CORS attributes.
     * <ul>
     * <li><b>cors.isCorsRequest:</b> Flag to determine if request is a CORS
     * request. Set to <code>true</code> if CORS request; <code>false</code>
     * otherwise.</li>
     * <li><b>cors.request.origin:</b> The Origin URL.</li>
     * <li><b>cors.request.type:</b> Type of request. Values:
     * <code>simple</code> or <code>preflight</code> or <code>not_cors</code> or
     * <code>invalid_cors</code></li>
     * <li><b>cors.request.headers:</b> Request headers sent as
     * 'Access-Control-Request-Headers' header, for pre-flight request.</li>
     * </ul>
     *
     * @param request         The {@link HttpServletRequest} object.
     * @param corsRequestType The {@link CORSRequestType} object.
     */
    protected static void decorateCORSProperties(
            final HttpServletRequest request,
            final CORSRequestType corsRequestType) {
        if (request == null) {
            throw new IllegalArgumentException(
                    SM.getString("corsFilter.nullRequest"));
        }

        if (corsRequestType == null) {
            throw new IllegalArgumentException(
                    SM.getString("corsFilter.nullRequestType"));
        }

        switch (corsRequestType) {
            case SIMPLE:
                request.setAttribute(
                        AbstractCorsFilter.HTTP_REQUEST_ATTRIBUTE_IS_CORS_REQUEST,
                        Boolean.TRUE);
                request.setAttribute(AbstractCorsFilter.HTTP_REQUEST_ATTRIBUTE_ORIGIN,
                        request.getHeader(AbstractCorsFilter.REQUEST_HEADER_ORIGIN));
                request.setAttribute(
                        AbstractCorsFilter.HTTP_REQUEST_ATTRIBUTE_REQUEST_TYPE,
                        corsRequestType.name().toLowerCase());
                break;
            case ACTUAL:
                request.setAttribute(
                        AbstractCorsFilter.HTTP_REQUEST_ATTRIBUTE_IS_CORS_REQUEST,
                        Boolean.TRUE);
                request.setAttribute(AbstractCorsFilter.HTTP_REQUEST_ATTRIBUTE_ORIGIN,
                        request.getHeader(AbstractCorsFilter.REQUEST_HEADER_ORIGIN));
                request.setAttribute(
                        AbstractCorsFilter.HTTP_REQUEST_ATTRIBUTE_REQUEST_TYPE,
                        corsRequestType.name().toLowerCase());
                break;
            case PRE_FLIGHT:
                request.setAttribute(
                        AbstractCorsFilter.HTTP_REQUEST_ATTRIBUTE_IS_CORS_REQUEST,
                        Boolean.TRUE);
                request.setAttribute(AbstractCorsFilter.HTTP_REQUEST_ATTRIBUTE_ORIGIN,
                        request.getHeader(AbstractCorsFilter.REQUEST_HEADER_ORIGIN));
                request.setAttribute(
                        AbstractCorsFilter.HTTP_REQUEST_ATTRIBUTE_REQUEST_TYPE,
                        corsRequestType.name().toLowerCase());
                String headers = request.getHeader(
                        REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS);
                if (headers == null) {
                    headers = "";
                }
                request.setAttribute(
                        AbstractCorsFilter.HTTP_REQUEST_ATTRIBUTE_REQUEST_HEADERS, headers);
                break;
            case NOT_CORS:
                request.setAttribute(
                        AbstractCorsFilter.HTTP_REQUEST_ATTRIBUTE_IS_CORS_REQUEST,
                        Boolean.FALSE);
                break;
            default:
                // Don't set any attributes
                break;
        }
    }


    /**
     * Joins elements of {@link Set} into a string, where each element is
     * separated by the provided separator.
     *
     * @param elements      The {@link Set} containing elements to join together.
     * @param joinSeparator The character to be used for separating elements.
     * @return The joined {@link String}; <code>null</code> if elements
     * {@link Set} is null.
     */
    protected static String join(final Collection<String> elements,
                                 final String joinSeparator) {
        String separator = ",";
        if (elements == null) {
            return null;
        }
        if (joinSeparator != null) {
            separator = joinSeparator;
        }
        StringBuilder buffer = new StringBuilder();
        boolean isFirst = true;
        for (String element : elements) {
            if (!isFirst) {
                buffer.append(separator);
            } else {
                isFirst = false;
            }

            if (element != null) {
                buffer.append(element);
            }
        }

        return buffer.toString();
    }


    /**
     * Determines the request type.
     *
     * @param request
     */
    protected CORSRequestType checkRequestType(final HttpServletRequest request) {
        CORSRequestType requestType = CORSRequestType.INVALID_CORS;
        if (request == null) {
            throw new IllegalArgumentException(
                    SM.getString("corsFilter.nullRequest"));
        }
        String originHeader = request.getHeader(REQUEST_HEADER_ORIGIN);
        // Section 6.1.1 and Section 6.2.1
        if (originHeader != null) {
            if (originHeader.isEmpty()) {
                requestType = CORSRequestType.INVALID_CORS;
            } else if (!isValidOrigin(originHeader)) {
                requestType = CORSRequestType.INVALID_CORS;
            } else {
                String method = request.getMethod();
                if (method != null && HTTP_METHODS.contains(method)) {
                    if ("OPTIONS".equals(method)) {
                        String accessControlRequestMethodHeader =
                                request.getHeader(
                                        REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD);
                        if (accessControlRequestMethodHeader != null
                                && !accessControlRequestMethodHeader.isEmpty()) {
                            requestType = CORSRequestType.PRE_FLIGHT;
                        } else if (accessControlRequestMethodHeader != null
                                && accessControlRequestMethodHeader.isEmpty()) {
                            requestType = CORSRequestType.INVALID_CORS;
                        } else {
                            requestType = CORSRequestType.ACTUAL;
                        }
                    } else if ("GET".equals(method) || "HEAD".equals(method)) {
                        requestType = CORSRequestType.SIMPLE;
                    } else if ("POST".equals(method)) {
                        String contentType = request.getContentType();
                        if (contentType != null) {
                            contentType = contentType.toLowerCase().trim();
                            if (SIMPLE_HTTP_REQUEST_CONTENT_TYPE_VALUES
                                    .contains(contentType)) {
                                requestType = CORSRequestType.SIMPLE;
                            } else {
                                requestType = CORSRequestType.ACTUAL;
                            }
                        }
                    } else if (COMPLEX_HTTP_METHODS.contains(method)) {
                        requestType = CORSRequestType.ACTUAL;
                    }
                }
            }
        } else {
            requestType = CORSRequestType.NOT_CORS;
        }

        return requestType;
    }


    /**
     * Checks if the Origin is allowed to make a CORS request.
     *
     * @param origin The Origin.
     * @return <code>true</code> if origin is allowed; <code>false</code>
     * otherwise.
     */
    private boolean isOriginAllowed(final String origin) {
        if (isAnyOriginAllowed()) {
            return true;
        }

        // If 'Origin' header is a case-sensitive match of any of allowed
        // origins, then return true, else return false.
        return allowedOrigins.contains(origin);
    }


    /**
     * Parses each param-value and populates configuration variables. If a param
     * is provided, it overrides the default.
     *
     * @param allowedOrigins      A {@link String} of comma separated origins.
     * @param allowedHttpMethods  A {@link String} of comma separated HTTP methods.
     * @param allowedHttpHeaders  A {@link String} of comma separated HTTP headers.
     * @param exposedHeaders      A {@link String} of comma separated headers that needs to be
     *                            exposed.
     * @param supportsCredentials "true" if support credentials needs to be enabled.
     * @param preflightMaxAge     The amount of seconds the user agent is allowed to cache the
     *                            result of the pre-flight request.
     * @throws ServletException
     */
    protected void parseAndStore(final String allowedOrigins,
                                 final String allowedHttpMethods, final String allowedHttpHeaders,
                                 final String exposedHeaders, final String supportsCredentials,
                                 final String preflightMaxAge, final String decorateRequest)
            throws ServletException {
        if (allowedOrigins != null) {
            if (!allowedOrigins.trim().equals("*")) {
                Set<String> setAllowedOrigins =
                        parseStringToSet(allowedOrigins);
                this.allowedOrigins.clear();
                this.allowedOrigins.addAll(setAllowedOrigins);
            }
        }

        if (allowedHttpMethods != null) {
            Set<String> setAllowedHttpMethods =
                    parseStringToSet(allowedHttpMethods);
            this.allowedHttpMethods.clear();
            this.allowedHttpMethods.addAll(setAllowedHttpMethods);
        }

        if (allowedHttpHeaders != null) {
            Set<String> setAllowedHttpHeaders =
                    parseStringToSet(allowedHttpHeaders);
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
            this.supportsCredentials = Boolean
                    .parseBoolean(supportsCredentials);
        }

        if (preflightMaxAge != null) {
            try {
                if (!preflightMaxAge.isEmpty()) {
                    this.preflightMaxAge = Long.parseLong(preflightMaxAge);
                } else {
                    this.preflightMaxAge = 0L;
                }
            } catch (NumberFormatException e) {
                throw new ServletException(
                        SM.getString("corsFilter.invalidPreflightMaxAge"), e);
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
            splits = new String[]{};
        }

        Set<String> set = new HashSet<String>();
        if (splits.length > 0) {
            for (String split : splits) {
                set.add(split.trim());
            }
        }

        return set;
    }


    /**
     * Checks if a given origin is valid or not. Criteria:
     * <ul>
     * <li>If an encoded character is present in origin, it's not valid.</li>
     * <li>Origin should be a valid {@link URI}</li>
     * </ul>
     *
     * @param origin
     * @see <a href="http://tools.ietf.org/html/rfc952">RFC952</a>
     */
    protected static boolean isValidOrigin(String origin) {
        // Checks for encoded characters. Helps prevent CRLF injection.
        if (origin.contains("%")) {
            return false;
        }

        URI originURI;

        try {
            originURI = new URI(origin);
        } catch (URISyntaxException e) {
            return false;
        }
        // If scheme for URI is null, return false. Return true otherwise.
        return originURI.getScheme() != null;

    }


    /**
     * Determines if any origin is allowed to make CORS request.
     *
     * @return <code>true</code> if it's enabled; false otherwise.
     */
    public boolean isAnyOriginAllowed() {
        if (allowedOrigins != null && allowedOrigins.size() == 0) {
            return true;
        }

        return false;
    }


    /**
     * Returns a {@link Set} of headers that should be exposed by browser.
     */
    public Collection<String> getExposedHeaders() {
        return exposedHeaders;
    }


    /**
     * Determines is supports credentials is enabled.
     */
    public boolean isSupportsCredentials() {
        return supportsCredentials;
    }


    /**
     * Returns the preflight response cache time in seconds.
     *
     * @return Time to cache in seconds.
     */
    public long getPreflightMaxAge() {
        return preflightMaxAge;
    }


    /**
     * Returns the {@link Set} of allowed origins that are allowed to make
     * requests.
     *
     * @return {@link Set}
     */
    public Collection<String> getAllowedOrigins() {
        return allowedOrigins;
    }

    /**
     * Sets the {@link Set} of allowed origins that are allowed to make requests.
     *
     * @param allowedOrigins {@link Set}
     */
    public void setAllowedOrigins(Collection<String> allowedOrigins) {
        this.allowedOrigins = allowedOrigins;
    }

    /**
     * Returns a {@link Set} of HTTP methods that are allowed to make requests.
     *
     * @return {@link Set}
     */
    public Collection<String> getAllowedHttpMethods() {
        return allowedHttpMethods;
    }


    /**
     * Returns a {@link Set} of headers support by resource.
     *
     * @return {@link Set}
     */
    public Collection<String> getAllowedHttpHeaders() {
        return allowedHttpHeaders;
    }


    // -------------------------------------------------- CORS Response Headers
    /**
     * The Access-Control-Allow-Origin header indicates whether a resource can
     * be shared based by returning the value of the Origin request header in
     * the response.
     */
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN =
            "Access-Control-Allow-Origin";

    /**
     * The Access-Control-Allow-Credentials header indicates whether the
     * response to request can be exposed when the omit credentials flag is
     * unset. When part of the response to a preflight request it indicates that
     * the actual request can include user credentials.
     */
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_CREDENTIALS =
            "Access-Control-Allow-Credentials";

    /**
     * The Access-Control-Expose-Headers header indicates which headers are safe
     * to expose to the API of a CORS API specification
     */
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_EXPOSE_HEADERS =
            "Access-Control-Expose-Headers";

    /**
     * The Access-Control-Max-Age header indicates how long the results of a
     * preflight request can be cached in a preflight result cache.
     */
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_MAX_AGE =
            "Access-Control-Max-Age";

    /**
     * The Access-Control-Allow-Methods header indicates, as part of the
     * response to a preflight request, which methods can be used during the
     * actual request.
     */
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_METHODS =
            "Access-Control-Allow-Methods";

    /**
     * The Access-Control-Allow-Headers header indicates, as part of the
     * response to a preflight request, which header field names can be used
     * during the actual request.
     */
    public static final String RESPONSE_HEADER_ACCESS_CONTROL_ALLOW_HEADERS =
            "Access-Control-Allow-Headers";

    // -------------------------------------------------- CORS Request Headers
    /**
     * The Origin header indicates where the cross-origin request or preflight
     * request originates from.
     */
    public static final String REQUEST_HEADER_ORIGIN = "Origin";

    /**
     * The Access-Control-Request-Method header indicates which method will be
     * used in the actual request as part of the preflight request.
     */
    public static final String REQUEST_HEADER_ACCESS_CONTROL_REQUEST_METHOD =
            "Access-Control-Request-Method";

    /**
     * The Access-Control-Request-Headers header indicates which headers will be
     * used in the actual request as part of the preflight request.
     */
    public static final String REQUEST_HEADER_ACCESS_CONTROL_REQUEST_HEADERS =
            "Access-Control-Request-Headers";

    // ----------------------------------------------------- Request attributes
    /**
     * The prefix to a CORS request attribute.
     */
    public static final String HTTP_REQUEST_ATTRIBUTE_PREFIX = "cors.";

    /**
     * Attribute that contains the origin of the request.
     */
    public static final String HTTP_REQUEST_ATTRIBUTE_ORIGIN =
            HTTP_REQUEST_ATTRIBUTE_PREFIX + "request.origin";

    /**
     * Boolean value, suggesting if the request is a CORS request or not.
     */
    public static final String HTTP_REQUEST_ATTRIBUTE_IS_CORS_REQUEST =
            HTTP_REQUEST_ATTRIBUTE_PREFIX + "isCorsRequest";

    /**
     * Type of CORS request, of type {@link CORSRequestType}.
     */
    public static final String HTTP_REQUEST_ATTRIBUTE_REQUEST_TYPE =
            HTTP_REQUEST_ATTRIBUTE_PREFIX + "request.type";

    /**
     * Request headers sent as 'Access-Control-Request-Headers' header, for
     * pre-flight request.
     */
    public static final String HTTP_REQUEST_ATTRIBUTE_REQUEST_HEADERS =
            HTTP_REQUEST_ATTRIBUTE_PREFIX + "request.headers";

    // -------------------------------------------------------------- Constants

    /**
     * Enumerates varies types of CORS requests. Also, provides utility methods
     * to determine the request type.
     */
    protected enum CORSRequestType {
        /**
         * A simple HTTP request, i.e. it shouldn't be pre-flighted.
         */
        SIMPLE,
        /**
         * A HTTP request that needs to be pre-flighted.
         */
        ACTUAL,
        /**
         * A pre-flight CORS request, to get meta information, before a
         * non-simple HTTP request is sent.
         */
        PRE_FLIGHT,
        /**
         * Not a CORS request, but a normal request.
         */
        NOT_CORS,
        /**
         * An invalid CORS request, i.e. it qualifies to be a CORS request, but
         * fails to be a valid one.
         */
        INVALID_CORS
    }

    /**
     * {@link Collection} of HTTP methods. Case sensitive.
     *
     * @see <a href="http://tools.ietf.org/html/rfc2616#section-5.1.1"
     * >http://tools.ietf.org/html/rfc2616#section-5.1.1</a>
     */
    public static final Collection<String> HTTP_METHODS =
            new HashSet<String>(Arrays.asList("OPTIONS", "GET", "HEAD", "POST",
                    "PUT", "DELETE", "TRACE", "CONNECT"));
    /**
     * {@link Collection} of non-simple HTTP methods. Case sensitive.
     */
    public static final Collection<String> COMPLEX_HTTP_METHODS =
            new HashSet<String>(Arrays.asList("PUT", "DELETE", "TRACE",
                    "CONNECT"));
    /**
     * {@link Collection} of Simple HTTP methods. Case sensitive.
     *
     * @see <a href="http://www.w3.org/TR/cors/#terminology"
     * >http://www.w3.org/TR/cors/#terminology</a>
     */
    public static final Collection<String> SIMPLE_HTTP_METHODS =
            new HashSet<String>(Arrays.asList("GET", "POST", "HEAD"));

    /**
     * {@link Collection} of Simple HTTP request headers. Case in-sensitive.
     *
     * @see <a href="http://www.w3.org/TR/cors/#terminology"
     * >http://www.w3.org/TR/cors/#terminology</a>
     */
    public static final Collection<String> SIMPLE_HTTP_REQUEST_HEADERS =
            new HashSet<String>(Arrays.asList("Accept", "Accept-Language",
                    "Content-Language"));

    /**
     * {@link Collection} of Simple HTTP request headers. Case in-sensitive.
     *
     * @see <a href="http://www.w3.org/TR/cors/#terminology"
     * >http://www.w3.org/TR/cors/#terminology</a>
     */
    public static final Collection<String> SIMPLE_HTTP_RESPONSE_HEADERS =
            new HashSet<String>(Arrays.asList("Cache-Control",
                    "Content-Language", "Content-Type", "Expires",
                    "Last-Modified", "Pragma"));

    /**
     * {@link Collection} of Simple HTTP request headers. Case in-sensitive.
     *
     * @see <a href="http://www.w3.org/TR/cors/#terminology"
     * >http://www.w3.org/TR/cors/#terminology</a>
     */
    public static final Collection<String> SIMPLE_HTTP_REQUEST_CONTENT_TYPE_VALUES =
            new HashSet<String>(Arrays.asList(
                    "application/x-www-form-urlencoded",
                    "multipart/form-data", "text/plain"));

    // ------------------------------------------------ Configuration Defaults
    /**
     * By default, all origins are allowed to make requests.
     */
    public static final String DEFAULT_ALLOWED_ORIGINS = "*";

    /**
     * By default, following methods are supported: GET, POST, HEAD and OPTIONS.
     */
    public static final String DEFAULT_ALLOWED_HTTP_METHODS =
            "GET,POST,HEAD,OPTIONS";

    /**
     * By default, time duration to cache pre-flight response is 30 mins.
     */
    public static final String DEFAULT_PREFLIGHT_MAXAGE = "1800";

    /**
     * By default, support credentials is turned on.
     */
    public static final String DEFAULT_SUPPORTS_CREDENTIALS = "true";

    /**
     * By default, following headers are supported:
     * Origin,Accept,X-Requested-With, Content-Type,
     * Access-Control-Request-Method, and Access-Control-Request-Headers.
     */
    public static final String DEFAULT_ALLOWED_HTTP_HEADERS =
            "Origin,Accept,X-Requested-With,Content-Type,"
                    + "Access-Control-Request-Method,Access-Control-Request-Headers";

    /**
     * By default, none of the headers are exposed in response.
     */
    public static final String DEFAULT_EXPOSED_HEADERS = "";

    /**
     * By default, request is decorated with CORS attributes.
     */
    public static final String DEFAULT_DECORATE_REQUEST = "true";

    // ----------------------------------------Filter Config Init param-name(s)
    /**
     * Key to retrieve if filter enabled from {@link FilterConfig}.
     */
    public static final String PARAM_CORS_ENABLED =
            "cors.enabled";

    /**
     * Key to retrieve allowed origins from {@link FilterConfig}.
     */
    public static final String PARAM_CORS_ALLOWED_ORIGINS =
            "cors.allowed.origins";

    /**
     * Key to retrieve support credentials from {@link FilterConfig}.
     */
    public static final String PARAM_CORS_SUPPORT_CREDENTIALS =
            "cors.support.credentials";

    /**
     * Key to retrieve exposed headers from {@link FilterConfig}.
     */
    public static final String PARAM_CORS_EXPOSED_HEADERS =
            "cors.exposed.headers";

    /**
     * Key to retrieve allowed headers from {@link FilterConfig}.
     */
    public static final String PARAM_CORS_ALLOWED_HEADERS =
            "cors.allowed.headers";

    /**
     * Key to retrieve allowed methods from {@link FilterConfig}.
     */
    public static final String PARAM_CORS_ALLOWED_METHODS =
            "cors.allowed.methods";

    /**
     * Key to retrieve preflight max age from {@link FilterConfig}.
     */
    public static final String PARAM_CORS_PREFLIGHT_MAXAGE =
            "cors.preflight.maxage";

    /**
     * Key to determine if request should be decorated.
     */
    public static final String PARAM_CORS_REQUEST_DECORATE =
            "cors.request.decorate";

}

final class StringManager {

    private static int LOCALE_CACHE_SIZE = 10;

    /**
     * The ResourceBundle for this StringManager.
     */
    private final ResourceBundle bundle;
    private final Locale locale;

    /**
     * Creates a new StringManager for a given package. This is a
     * private method and all access to it is arbitrated by the
     * static getManager method call so that only one StringManager
     * per package will be created.
     *
     * @param packageName Name of package to create StringManager for.
     */
    private StringManager(String packageName, Locale locale) {
        String bundleName = packageName + ".LocalStrings";
        ResourceBundle bnd = null;
        try {
            bnd = ResourceBundle.getBundle(bundleName, locale);
        } catch (MissingResourceException ex) {
            // Try from the current loader (that's the case for trusted apps)
            // Should only be required if using a TC5 style classloader structure
            // where common != shared != server
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
            if (cl != null) {
                try {
                    bnd = ResourceBundle.getBundle(bundleName, locale, cl);
                } catch (MissingResourceException ex2) {
                    // Ignore
                }
            }
        }
        bundle = bnd;
        // Get the actual locale, which may be different from the requested one
        if (bundle != null) {
            Locale bundleLocale = bundle.getLocale();
            if (bundleLocale.equals(Locale.ROOT)) {
                this.locale = Locale.ENGLISH;
            } else {
                this.locale = bundleLocale;
            }
        } else {
            this.locale = null;
        }
    }

    /**
     * Get a string from the underlying resource bundle or return
     * null if the String is not found.
     *
     * @param key to desired resource String
     * @return resource String matching <i>key</i> from underlying
     * bundle or null if not found.
     * @throws IllegalArgumentException if <i>key</i> is null.
     */
    public String getString(String key) {
        if (key == null) {
            String msg = "key may not have a null value";

            throw new IllegalArgumentException(msg);
        }

        String str = null;

        try {
            // Avoid NPE if bundle is null and treat it like an MRE
            if (bundle != null) {
                str = bundle.getString(key);
            }
        } catch (MissingResourceException mre) {
            //bad: shouldn't mask an exception the following way:
            //   str = "[cannot find message associated with key '" + key +
            //         "' due to " + mre + "]";
            //     because it hides the fact that the String was missing
            //     from the calling code.
            //good: could just throw the exception (or wrap it in another)
            //      but that would probably cause much havoc on existing
            //      code.
            //better: consistent with container pattern to
            //      simply return null.  Calling code can then do
            //      a null check.
            str = null;
        }

        return str;
    }

    /**
     * Get a string from the underlying resource bundle and format
     * it with the given set of arguments.
     *
     * @param key
     * @param args
     */
    public String getString(final String key, final Object... args) {
        String value = getString(key);
        if (value == null) {
            value = key;
        }

        MessageFormat mf = new MessageFormat(value);
        mf.setLocale(locale);
        return mf.format(args, new StringBuffer(), null).toString();
    }

    /**
     * Identify the Locale this StringManager is associated with
     */
    public Locale getLocale() {
        return locale;
    }

    // --------------------------------------------------------------
    // STATIC SUPPORT METHODS
    // --------------------------------------------------------------

    private static final Map<String, Map<Locale, StringManager>> MANAGERS =
            new Hashtable<String, Map<Locale, StringManager>>();

    /**
     * Get the StringManager for a particular package. If a manager for
     * a package already exists, it will be reused, else a new
     * StringManager will be created and returned.
     *
     * @param packageName The package name
     */
    public static synchronized StringManager getManager(
            String packageName) {
        return getManager(packageName, Locale.getDefault());
    }

    /**
     * Get the StringManager for a particular package and Locale. If a manager
     * for a package/Locale combination already exists, it will be reused, else
     * a new StringManager will be created and returned.
     *
     * @param packageName The package name
     * @param locale      The Locale
     */
    public static synchronized StringManager getManager(
            String packageName, Locale locale) {

        Map<Locale, StringManager> map = MANAGERS.get(packageName);
        if (map == null) {
            /*
             * Don't want the HashMap to be expanded beyond LOCALE_CACHE_SIZE.
             * Expansion occurs when size() exceeds capacity. Therefore keep
             * size at or below capacity.
             * removeEldestEntry() executes after insertion therefore the test
             * for removal needs to use one less than the maximum desired size
             *
             */
            map = new LinkedHashMap<Locale, StringManager>(LOCALE_CACHE_SIZE, 1, true) {
                private static final long serialVersionUID = 1L;

                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<Locale, StringManager> eldest) {
                    if (size() > (LOCALE_CACHE_SIZE - 1)) {
                        return true;
                    }
                    return false;
                }
            };
            MANAGERS.put(packageName, map);
        }

        StringManager mgr = map.get(locale);
        if (mgr == null) {
            mgr = new StringManager(packageName, locale);
            map.put(locale, mgr);
        }
        return mgr;
    }

    /**
     * Retrieve the StringManager for a list of Locales. The first StringManager
     * found will be returned.
     *
     * @param requestedLocales the list of Locales
     * @return the found StringManager or the default StringManager
     */
    public static StringManager getManager(String packageName,
                                           Enumeration<Locale> requestedLocales) {
        while (requestedLocales.hasMoreElements()) {
            Locale locale = requestedLocales.nextElement();
            StringManager result = getManager(packageName, locale);
            if (result.getLocale().equals(locale)) {
                return result;
            }
        }
        // Return the default
        return getManager(packageName);
    }
}

final class Constants {

    private Constants() { }

    public static final String PACKAGE = "org.apache.catalina.filters";

    public static final String CSRF_NONCE_SESSION_ATTR_NAME =
            "org.apache.catalina.filters.CSRF_NONCE";

    public static final String CSRF_NONCE_REQUEST_PARAM =
            "org.apache.catalina.filters.CSRF_NONCE";

    public static final String METHOD_GET = "GET";

    public static final String CSRF_REST_NONCE_HEADER_NAME = "X-CSRF-Token";

    public static final String CSRF_REST_NONCE_HEADER_FETCH_VALUE = "Fetch";

    public static final String CSRF_REST_NONCE_HEADER_REQUIRED_VALUE = "Required";

    public static final String CSRF_REST_NONCE_SESSION_ATTR_NAME =
            "org.apache.catalina.filters.CSRF_REST_NONCE";
}
