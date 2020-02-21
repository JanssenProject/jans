package org.gluu.oxd.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.apache.commons.collections.CollectionUtils;
import org.gluu.oxauth.model.uma.JsonLogicNodeParser;
import org.gluu.oxauth.model.uma.PermissionTicket;
import org.gluu.oxd.common.*;
import org.gluu.oxd.common.introspection.CorrectRptIntrospectionResponse;
import org.gluu.oxd.common.introspection.CorrectUmaPermission;
import org.gluu.oxd.common.params.RsCheckAccessParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.RsCheckAccessResponse;
import org.gluu.oxd.rs.protect.resteasy.PatProvider;
import org.gluu.oxd.rs.protect.resteasy.ResourceRegistrar;
import org.gluu.oxd.rs.protect.resteasy.RptPreProcessInterceptor;
import org.gluu.oxd.rs.protect.resteasy.ServiceProvider;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.model.UmaResource;
import org.gluu.oxd.server.service.Rp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

public class RsCheckAccessOperation extends BaseOperation<RsCheckAccessParams> {

    private static final Logger LOG = LoggerFactory.getLogger(RsCheckAccessOperation.class);

    /**
     * Constructor
     *
     * @param command command
     */
    RsCheckAccessOperation(Command command, final Injector injector) {
        super(command, injector, RsCheckAccessParams.class);
    }

    @Override
    public IOpResponse execute(final RsCheckAccessParams params) throws Exception {
        validate(params);

        Rp rp = getRp();
        UmaResource resource = rp.umaResource(params.getPath(), params.getHttpMethod());
        if (resource == null) {
            final ErrorResponse error = new ErrorResponse("invalid_request");
            error.setErrorDescription("Resource is not protected with path: " + params.getPath() + " and httpMethod: " + params.getHttpMethod() +
                    ". Please protect your resource first with uma_rs_protect command. Check details on " + CoreUtils.DOC_URL);
            LOG.error(error.getErrorDescription());
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(Jackson2.asJson(error))
                    .build());
        }

        PatProvider patProvider = new PatProvider() {
            @Override
            public String getPatToken() {
                return getUmaTokenService().getPat(params.getOxdId()).getToken();
            }

            @Override
            public void clearPat() {
                // do nothing
            }
        };

        List<String> requiredScopes = getRequiredScopes(params, resource);

        CorrectRptIntrospectionResponse status = getIntrospectionService().introspectRpt(params.getOxdId(), params.getRpt());

        LOG.trace("RPT: " + params.getRpt() + ", status: " + status);

        if (!Strings.isNullOrEmpty(params.getRpt()) && status != null && status.getActive() && status.getPermissions() != null) {
            for (CorrectUmaPermission permission : status.getPermissions()) {
                boolean containsAny = !Collections.disjoint(requiredScopes, permission.getScopes());

                LOG.trace("containsAny: " + containsAny + ", requiredScopes: " + requiredScopes + ", permissionScopes: " + permission.getScopes());

                if (containsAny) {
                    if ((permission.getResourceId() != null && permission.getResourceId().equals(resource.getId()))) { // normal UMA
                        LOG.debug("RPT has enough permissions, access GRANTED. Path: " + params.getPath() + ", httpMethod:" + params.getHttpMethod() + ", site: " + rp);
                        return new RsCheckAccessResponse("granted");
                    }
                }
            }
        }


        if (CollectionUtils.isEmpty(params.getScopes()) && !CollectionUtils.isEmpty(resource.getTicketScopes())) {
            requiredScopes = resource.getTicketScopes();
        }

        final RptPreProcessInterceptor rptInterceptor = getOpClientFactory().createRptPreProcessInterceptor(new ResourceRegistrar(patProvider, new ServiceProvider(rp.getOpHost())));
        Response response = null;
        try {
            LOG.trace("Try to register ticket, scopes: " + requiredScopes + ", resourceId: " + resource.getId());
            response = rptInterceptor.registerTicketResponse(requiredScopes, resource.getId());
        } catch (ClientErrorException e) {
            LOG.debug("Failed to register ticket. Entity: " + e.getResponse().readEntity(String.class) + ", status: " + e.getResponse().getStatus(), e);
            if (e.getResponse().getStatus() == 400 || e.getResponse().getStatus() == 401) {
                LOG.debug("Try maybe PAT is lost on AS, force refresh PAT and request ticket again ...");
                getUmaTokenService().obtainPat(params.getOxdId()); // force to refresh PAT
                response = rptInterceptor.registerTicketResponse(requiredScopes, resource.getId());
            } else {
                throw e;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }

        RsCheckAccessResponse opResponse = new RsCheckAccessResponse("denied");
        opResponse.setWwwAuthenticateHeader((String) response.getMetadata().getFirst("WWW-Authenticate"));
        opResponse.setTicket(((PermissionTicket) response.getEntity()).getTicket());
        LOG.debug("Access denied for path: " + params.getPath() + " and httpMethod: " + params.getHttpMethod() + ". Ticket is registered: " + opResponse);

        return opResponse;
    }

    private List<String> getRequiredScopes(RsCheckAccessParams params, UmaResource resource) {

        List<String> resourceScopes = resource.getScopes();

        if (resourceScopes.isEmpty()) {
            LOG.trace("Not scopes in resource:" + resource + ", oxdId: " + params.getOxdId());
            if (!resource.getScopeExpressions().isEmpty() && JsonLogicNodeParser.isNodeValid(resource.getScopeExpressions().get(0))) {
                resourceScopes = JsonLogicNodeParser.parseNode(resource.getScopeExpressions().get(0)).getData();
                LOG.trace("Set requiredScope from scope expression.");
            }
        }

        if (!CollectionUtils.isEmpty(params.getScopes())) {
            return params.getScopes(); // we can't validate it because it can be spontaneous scope
        }
        return resourceScopes;
    }

    private void validate(RsCheckAccessParams params) {
        if (Strings.isNullOrEmpty(params.getHttpMethod())) {
            throw new HttpException(ErrorResponseCode.NO_UMA_HTTP_METHOD);
        }
        if (Strings.isNullOrEmpty(params.getPath())) {
            throw new HttpException(ErrorResponseCode.NO_UMA_PATH_PARAMETER);
        }
    }
}
