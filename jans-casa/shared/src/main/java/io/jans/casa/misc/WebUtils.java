package io.jans.casa.misc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.zk.ui.Executions;
import jakarta.servlet.http.Cookie;
import javax.imageio.ImageIO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URL;
import java.util.Locale;
import java.util.Optional;

/**
 * Provides utility methods for web-related functionalities. A good place to call these methods are ZK View Models or
 * ZK page initiators, that is, a context where there is a ZK Execution.
 * @author jgomer
 */
public final class WebUtils {

    private static Logger LOG = LoggerFactory.getLogger(WebUtils.class);

    /**
     * The page where users are redirected to after they have successfully logged in.
     */
    public static final String USER_PAGE_URL ="user.zul";
    public static final String ADMIN_PAGE_URL ="admin.zul";
    public static final Locale DEFAULT_LOCALE = Locale.ENGLISH;

    private WebUtils() { }

    /**
     * Gets the current {@link HttpServletRequest} object.
     * @return The servlet request
     */
    public static HttpServletRequest getServletRequest() {
        return (HttpServletRequest) Executions.getCurrent().getNativeRequest();
    }

    /**
     * Gets the current {@link HttpServletResponse} object.
     * @return The servlet response
     */
    public static HttpServletResponse getServletResponse() {
        return (HttpServletResponse) Executions.getCurrent().getNativeResponse();
    }

    /**
     * Gets the value of the selected header.
     * @param headerName Header name
     * @return Value of the header or null if the servlet request does not contain such header
     */
    public static String getRequestHeader(String headerName) {
        return getServletRequest().getHeader(headerName);
    }

    /**
     * Invalidates the current http session.
     * @param request An HttpServletRequest instance from which the session can be obtained
     */
    public static void invalidateSession(HttpServletRequest request) {
        Optional.ofNullable(request.getSession(false)).ifPresent(HttpSession::invalidate);
    }

    /**
     * Gets the IP address of the originator of the current servlet request.
     * @return A string value with the address (null if no IP could be obtained)
     */
    public static String getRemoteIP(){

        String ip = getRequestHeader("X-Forwarded-For");
        if (ip == null) {
            ip = getServletRequest().getRemoteAddr();
        } else {
            String[] ips = ip.split(",\\s*");
            ip = Utils.isNotEmpty(ips) ? ips[0] : null;
        }
        return ip;

    }

    /**
     * Performs a redirect to the url passed avoiding any UI rendering (see "Forward and redirect" in ZK developer's
     * reference manual). It is equivalent to calling <code>execRedirect(url, true)</code>
     * @param url A String where the browser will be redirected to
     */
    public static void execRedirect(String url){
        execRedirect(url, true);
    }

    /**
     * Performs a redirect to the url passed. The boolean parameter determines if the underlying ZK execution can be
     * voided (see "Forward and redirect" in ZK developer's reference manual).
     * @param url A String where the browser will be redirected to
     * @param voidUI Whether to void the execution
     */
    public static void execRedirect(String url, boolean voidUI) {

        try {
            HttpServletResponse response = getServletResponse();
            LOG.debug("Redirecting to URL={}", url);
            response.sendRedirect(response.encodeRedirectURL(url));
            if (voidUI) {
                Executions.getCurrent().setVoided(voidUI); //no need to create UI since redirect will take place
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }

    }

    /**
     * Gets the query parameter whose name is passed as parameter.
     * @param param Parameter name
     * @return The first value of the named parameter in the request, null if the parameter is not part of it
     */
    public static String getQueryParam(String param) {
        String[] values = Executions.getCurrent().getParameterValues(param);
        return Utils.isEmpty(values) ? null :  values[0];
    }

    /**
     * Reads the contents of the URL passes as param and returns null if the contents received are not image-like (e.g.
     * different file format or HTTP error)
     * @param url String with URL to inspect
     * @return The value received in parameter if it points to a valid image, null otherwise
     */
    public static String validateImageUrl(String url) {

        String val = null;
        try {
            val = ImageIO.read(new URL(url)) == null ? null : url;
        } catch (Exception e) {
            LOG.warn("Error validating image url '{}'", url);
            //LOG.error(e.getMessage(), e);
        }
        return val;

    }
    
    public static String getFullRequestURL() {
        HttpServletRequest req = getServletRequest();
        String query = req.getQueryString();
        StringBuffer sb = req.getRequestURL();
        if (query != null) {
            sb.append("?").append(query);
        }
        return sb.toString();
    }
    
    public static String getValueFromCookie( String cookieName) {
        try {
            final Cookie[] cookies = ((HttpServletRequest)Executions.getCurrent().getNativeRequest()).getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(cookieName) /*&& cookie.getSecure()*/) {
                    	LOG.info("Found cookie: '{}'", cookie.getValue());
                        return cookie.getValue();
                    }
                }
            }
        } catch (Exception e) {
        	LOG.error(e.getMessage(), e);
        }
        return "";
    }

}
