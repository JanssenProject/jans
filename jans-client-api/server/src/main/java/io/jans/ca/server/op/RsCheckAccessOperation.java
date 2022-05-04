package io.jans.ca.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import io.jans.as.model.uma.JsonLogicNodeParser;
import io.jans.as.model.uma.PermissionTicket;
import io.jans.ca.common.*;
import io.jans.ca.common.introspection.CorrectRptIntrospectionResponse;
import io.jans.ca.common.introspection.CorrectUmaPermission;
import io.jans.ca.common.params.RsCheckAccessParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.RsCheckAccessResponse;
import io.jans.ca.rs.protect.resteasy.PatProvider;
import io.jans.ca.rs.protect.resteasy.ResourceRegistrar;
import io.jans.ca.rs.protect.resteasy.RptPreProcessInterceptor;
import io.jans.ca.rs.protect.resteasy.ServiceProvider;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.model.UmaResource;
import io.jans.ca.server.service.Rp;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
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
                return getUmaTokenService().getPat(params.getRpId()).getToken();
            }

            @Override
            public void clearPat() {
                // do nothing
            }
        };

        List<String> requiredScopes = getRequiredScopes(params, resource);

        CorrectRptIntrospectionResponse status = getIntrospectionService().introspectRpt(params.getRpId(), params.getRpt());

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
                getUmaTokenService().obtainPat(params.getRpId()); // force to refresh PAT
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
            LOG.trace("Not scopes in resource:" + resource + ", rpId: " + params.getRpId());
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
