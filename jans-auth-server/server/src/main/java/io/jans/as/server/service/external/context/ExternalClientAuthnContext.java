package io.jans.as.server.service.external.context;

import io.jans.as.model.config.Constants;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.token.TokenErrorResponseType;
import io.jans.service.cdi.util.CdiUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.WebApplicationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Yuriy Z
 */
public class ExternalClientAuthnContext extends ExternalScriptContext {

    private static final Logger log = LoggerFactory.getLogger(ExternalClientAuthnContext.class);

    private String realm = "jans-auth";

    public ExternalClientAuthnContext(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        super(httpRequest, httpResponse);
    }

    public void sendUnauthorizedError() {
        ErrorResponseFactory errorResponseFactory = CdiUtil.bean(ErrorResponseFactory.class);
        try (PrintWriter out = httpResponse.getWriter()) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.addHeader(Constants.WWW_AUTHENTICATE, "Basic realm=\"" + getRealm() + "\"");
            httpResponse.setContentType(Constants.CONTENT_TYPE_APPLICATION_JSON_UTF_8);
            out.write(errorResponseFactory.errorAsJson(TokenErrorResponseType.INVALID_CLIENT, "Unable to authenticate client."));
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public void sendResponse(HttpServletResponse httpResponse, WebApplicationException e) {
        try (PrintWriter out = httpResponse.getWriter()) {
            httpResponse.setStatus(e.getResponse().getStatus());
            httpResponse.addHeader(Constants.WWW_AUTHENTICATE, "Basic realm=\"" + getRealm() + "\"");
            httpResponse.setContentType(Constants.CONTENT_TYPE_APPLICATION_JSON_UTF_8);
            out.write(e.getResponse().getEntity().toString());
        } catch (IOException ex) {
            log.error(ex.getMessage(), ex);
        }
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
}
