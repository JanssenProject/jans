package org.gluu.oxeleven.service;

import com.google.common.base.Strings;
import org.apache.log4j.Logger;
import org.gluu.oxeleven.model.Configuration;
import org.gluu.oxeleven.util.StringUtils;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.annotations.Startup;
import org.jboss.seam.annotations.intercept.BypassInterceptors;
import org.jboss.seam.annotations.web.Filter;
import org.jboss.seam.web.AbstractFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2017
 */
@Startup
@Filter
@Name("TestModeTokenFilter")
@Scope(ScopeType.APPLICATION)
@BypassInterceptors
public class TestModeTokenFilter extends AbstractFilter {

    private static final Logger LOG = Logger.getLogger(TestModeTokenFilter.class);
    private static final String oxElevenGenerateKeyEndpoint = "rest/oxeleven/generateKey";
    private static final String oxElevenSignEndpoint = "rest/oxeleven/sign";
    private static final String oxElevenVerifySignatureEndpoint = "rest/oxeleven/verifySignature";
    private static final String oxElevenDeleteKeyEndpoint = "rest/oxeleven/deleteKey";

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest) {
            HttpServletRequest httpServletRequest = (HttpServletRequest) request;
            String path = httpServletRequest.getRequestURL().toString();
            if (!Strings.isNullOrEmpty(path)) {
                if (path.endsWith(oxElevenGenerateKeyEndpoint)
                        || path.endsWith(oxElevenSignEndpoint)
                        || path.endsWith(oxElevenVerifySignatureEndpoint)
                        || path.endsWith(oxElevenDeleteKeyEndpoint)) {
                    if (httpServletRequest.getHeader("Authorization") != null) {
                        String header = httpServletRequest.getHeader("Authorization");
                        if (header.startsWith("Bearer ")) {
                            String accessToken = header.substring(7);
                            Configuration configuration = ConfigurationService.instance().getConfiguration();
                            String testModeToken = configuration.getTestModeToken();
                            if (!Strings.isNullOrEmpty(accessToken) && !Strings.isNullOrEmpty(testModeToken)
                                    && accessToken.equals(testModeToken)) {
                                chain.doFilter(request, response);
                                return;
                            }
                        }
                    }

                    sendError((HttpServletResponse) response);
                    return;
                }
            }
        }

        chain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse servletResponse) {
        PrintWriter out = null;
        try {
            out = servletResponse.getWriter();

            servletResponse.setStatus(401);
            servletResponse.addHeader("WWW-Authenticate", "Bearer");
            servletResponse.setContentType(MediaType.APPLICATION_JSON);
            out.write(StringUtils.getErrorResponse(
                    "unauthorized",
                    "The request is not authorized."
            ));
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        } catch (Exception ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }
    }
}
