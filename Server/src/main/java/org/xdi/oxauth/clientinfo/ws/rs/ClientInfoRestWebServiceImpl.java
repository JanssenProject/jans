package org.xdi.oxauth.clientinfo.ws.rs;

import java.util.Set;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.log.Log;
import org.xdi.model.GluuAttribute;
import org.xdi.oxauth.model.clientinfo.ClientInfoErrorResponseType;
import org.xdi.oxauth.model.clientinfo.ClientInfoParamsValidator;
import org.xdi.oxauth.model.common.AuthorizationGrant;
import org.xdi.oxauth.model.common.AuthorizationGrantList;
import org.xdi.oxauth.model.common.Scope;
import org.xdi.oxauth.model.error.ErrorResponseFactory;
import org.xdi.oxauth.model.registration.Client;
import org.xdi.oxauth.service.AttributeService;
import org.xdi.oxauth.service.ScopeService;

/**
 * Provides interface for Client Info REST web services
 *
 * @author Javier Rojas Blum Date: 07.19.2012
 */
@Name("requestClientInfoRestWebService")
public class ClientInfoRestWebServiceImpl implements ClientInfoRestWebService {

    @Logger
    private Log log;
    @In
    private ErrorResponseFactory errorResponseFactory;
    @In
    private AuthorizationGrantList authorizationGrantList;
    @In
    private ScopeService scopeService;
    @In
    private AttributeService attributeService;

    @Override
    public Response requestUserInfoGet(String accessToken, String authorization, SecurityContext securityContext) {
        return requestClientInfo(accessToken, authorization, securityContext);
    }

    @Override
    public Response requestUserInfoPost(String accessToken, String authorization, SecurityContext securityContext) {
        return requestClientInfo(accessToken, authorization, securityContext);
    }

    public Response requestClientInfo(String accessToken, String authorization, SecurityContext securityContext) {
        if (authorization != null && !authorization.isEmpty() && authorization.startsWith("Bearer ")) {
            accessToken = authorization.substring(7);
        }
        log.debug("Attempting to request Client Info, Access token = {0}, Is Secure = {1}",
                accessToken, securityContext.isSecure());
        Response.ResponseBuilder builder = Response.ok();

        if (!ClientInfoParamsValidator.validateParams(accessToken)) {
            builder = Response.status(400);
            builder.entity(errorResponseFactory.getErrorAsJson(ClientInfoErrorResponseType.INVALID_REQUEST));
        } else {
            AuthorizationGrant authorizationGrant = authorizationGrantList.getAuthorizationGrantByAccessToken(accessToken);

            if (authorizationGrant == null) {
                builder = Response.status(400);
                builder.entity(errorResponseFactory.getErrorAsJson(ClientInfoErrorResponseType.INVALID_TOKEN));
            } else {
                CacheControl cacheControl = new CacheControl();
                cacheControl.setPrivate(true);
                cacheControl.setNoTransform(false);
                cacheControl.setNoStore(true);
                builder.cacheControl(cacheControl);
                builder.header("Pragma", "no-cache");

                builder.entity(getJSonResponse(authorizationGrant.getClient(),
                        authorizationGrant.getScopes()));
            }
        }

        return builder.build();
    }

    /**
     * Builds a JSon String with the response parameters.
     */
    public String getJSonResponse(Client client, Set<String> scopes) {
//        FileConfiguration ldapConfiguration = ConfigurationFactory.getLdapConfiguration();
        JSONObject jsonObj = new JSONObject();

        try {
            for (String scopeName : scopes) {
                Scope scope = scopeService.getScopeByDisplayName(scopeName);

                if (scope.getOxAuthClaims() != null) {
                    for (String claimDn : scope.getOxAuthClaims()) {
                        GluuAttribute attribute = attributeService.getScopeByDn(claimDn);

                        String attributeName = attribute.getName();
                        Object attributeValue = client.getAttribute(attribute.getName());

                        jsonObj.put(attributeName, attributeValue);
                    }
                }
            }
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return jsonObj.toString();
    }
}