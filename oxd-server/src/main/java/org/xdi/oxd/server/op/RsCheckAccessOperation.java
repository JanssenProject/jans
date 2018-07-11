package org.xdi.oxd.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.codehaus.jackson.node.POJONode;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.model.uma.JsonLogicNodeParser;
import org.xdi.oxauth.model.uma.PermissionTicket;
import org.xdi.oxd.common.*;
import org.xdi.oxd.common.introspection.CorrectRptIntrospectionResponse;
import org.xdi.oxd.common.introspection.CorrectUmaPermission;
import org.xdi.oxd.common.params.RsCheckAccessParams;
import org.xdi.oxd.common.response.RsCheckAccessResponse;
import org.xdi.oxd.rs.protect.resteasy.PatProvider;
import org.xdi.oxd.rs.protect.resteasy.ResourceRegistrar;
import org.xdi.oxd.rs.protect.resteasy.RptPreProcessInterceptor;
import org.xdi.oxd.rs.protect.resteasy.ServiceProvider;
import org.xdi.oxd.server.model.UmaResource;
import org.xdi.oxd.server.service.Rp;

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
    protected RsCheckAccessOperation(Command command, final Injector injector) {
        super(command, injector, RsCheckAccessParams.class);
    }

    @Override
    public CommandResponse execute(final RsCheckAccessParams params) throws Exception {
        validate(params);

        Rp site = getRp();
        UmaResource resource = site.umaResource(params.getPath(), params.getHttpMethod());
        if (resource == null) {
            final ErrorResponse error = new ErrorResponse("invalid_request");
            error.setErrorDescription("Resource is not protected with path: " + params.getPath() + " and httpMethod: " + params.getHttpMethod() +
                    ". Please protect your resource first with uma_rs_protect command. Check details on " + CoreUtils.DOC_URL);
            LOG.error(error.getErrorDescription());
            return CommandResponse.error().setData(new POJONode(error));
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

        CorrectRptIntrospectionResponse status = getIntrospectionService().introspectRpt(params.getOxdId(), params.getRpt());

        LOG.trace("RPT: " + params.getRpt() + ", status: " + status);

        if (!Strings.isNullOrEmpty(params.getRpt()) && status != null && status.getActive() && status.getPermissions() != null) {
            for (CorrectUmaPermission permission : status.getPermissions()) {
                List<String> requiredScopes = resource.getScopes();

                if (requiredScopes.isEmpty()) {
                    LOG.trace("Not scopes in resource:" + resource + ", oxdId: " + params.getOxdId());
                    if (!resource.getScopeExpressions().isEmpty() && JsonLogicNodeParser.isNodeValid(resource.getScopeExpressions().get(0))) {
                        requiredScopes = JsonLogicNodeParser.parseNode(resource.getScopeExpressions().get(0)).getData();
                        LOG.trace("Set requiredScope from scope expression.");
                    }
                }

                boolean containsAny = !Collections.disjoint(requiredScopes, permission.getScopes());

                LOG.trace("containsAny: " + containsAny + ", requiredScopes: " + requiredScopes + ", permissionScopes: " + permission.getScopes());

                if (containsAny) {
                    if ((permission.getResourceId() != null && permission.getResourceId().equals(resource.getId()))) { // normal UMA
                        LOG.debug("RPT has enough permissions, access GRANTED. Path: " + params.getPath() + ", httpMethod:" + params.getHttpMethod() + ", site: " + site);
                        return okResponse(new RsCheckAccessResponse("granted"));
                    }
                }
            }
        }

        List<String> scopes = resource.getTicketScopes();
        if (scopes.isEmpty()) {
            scopes = resource.getScopes();
        }

        final RptPreProcessInterceptor rptInterceptor = new RptPreProcessInterceptor(new ResourceRegistrar(patProvider, new ServiceProvider(site.getOpHost())));
        Response response = null;
        try {
            LOG.trace("Try to register ticket, scopes: " + scopes + ", resourceId: " + resource.getId());
            response = rptInterceptor.registerTicketResponse(scopes, resource.getId());
        } catch (ClientResponseFailure e) {
            LOG.debug("Failed to register ticket. Entity: " + e.getResponse().getEntity(String.class) + ", status: " + e.getResponse().getStatus(), e);
            if (e.getResponse().getStatus() == 400 || e.getResponse().getStatus() == 401) {
                LOG.debug("Try maybe PAT is lost on AS, force refresh PAT and request ticket again ...");
                getUmaTokenService().obtainPat(params.getOxdId()); // force to refresh PAT
                response = rptInterceptor.registerTicketResponse(scopes, resource.getId());
            } else {
                throw e;
            }
        }

        RsCheckAccessResponse opResponse = new RsCheckAccessResponse("denied");
        opResponse.setWwwAuthenticateHeader((String) response.getMetadata().getFirst("WWW-Authenticate"));
        opResponse.setTicket(((PermissionTicket) response.getEntity()).getTicket());
        LOG.debug("Access denied for path: " + params.getPath() + " and httpMethod: " + params.getHttpMethod() + ". Ticket is registered: " + opResponse);

        return okResponse(opResponse);
    }

    private void validate(RsCheckAccessParams params) {
        if (Strings.isNullOrEmpty(params.getHttpMethod())) {
            throw new ErrorResponseException(ErrorResponseCode.NO_UMA_HTTP_METHOD);
        }
        if (Strings.isNullOrEmpty(params.getPath())) {
            throw new ErrorResponseException(ErrorResponseCode.NO_UMA_PATH_PARAMETER);
        }
    }
}
