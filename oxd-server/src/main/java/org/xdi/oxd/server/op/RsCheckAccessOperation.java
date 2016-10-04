package org.xdi.oxd.server.op;

import com.google.common.base.Strings;
import com.google.inject.Injector;
import org.codehaus.jackson.node.POJONode;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.jboss.resteasy.core.ServerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.uma.RptStatusService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.PermissionTicket;
import org.xdi.oxauth.model.uma.RptIntrospectionResponse;
import org.xdi.oxauth.model.uma.UmaPermission;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.RsCheckAccessParams;
import org.xdi.oxd.common.response.RsCheckAccessResponse;
import org.xdi.oxd.rs.protect.resteasy.PatProvider;
import org.xdi.oxd.rs.protect.resteasy.ResourceRegistrar;
import org.xdi.oxd.rs.protect.resteasy.RptPreProcessInterceptor;
import org.xdi.oxd.rs.protect.resteasy.ServiceProvider;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.model.UmaResource;
import org.xdi.oxd.server.service.SiteConfiguration;

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

        SiteConfiguration site = getSite();
        UmaResource resource = site.umaResource(params.getPath(), params.getHttpMethod());
        if (resource == null) {
            final ErrorResponse error = new ErrorResponse("invalid_request");
            error.setErrorDescription("Resource is not protected with path: " + params.getPath() + " and httpMethod: " + params.getHttpMethod() +
                    ". Please protect your resource first with uma_rs_protect command. Check details on " + Configuration.DOC_URL);
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

        final RptStatusService registrationService = UmaClientFactory.instance().createRptStatusService(getDiscoveryService().getUmaDiscoveryByOxdId(params.getOxdId()), getHttpService().getClientExecutor());

        RptIntrospectionResponse status;
        try {
            status = registrationService.requestRptStatus("Bearer " + patProvider.getPatToken(), params.getRpt(), "");
        } catch (ClientResponseFailure e) {
            int httpStatus = e.getResponse().getStatus();
            if (httpStatus == 401 || httpStatus == 400 || httpStatus == 403) {
                String freshPat = getUmaTokenService().obtainPat(params.getOxdId()).getToken();
                status = registrationService.requestRptStatus("Bearer " + freshPat, params.getRpt(), "");
            } else {
                throw e;
            }
        }

        LOG.trace("RPT: " + params.getRpt() + ", status: " + status);

        final boolean isGat = RptPreProcessInterceptor.isGat(params.getRpt());
        if (!Strings.isNullOrEmpty(params.getRpt()) && status != null && status.getActive() && status.getPermissions() != null) {
            for (UmaPermission permission : status.getPermissions()) {
                final List<String> requiredScopes = resource.getScopes();
                boolean containsAny = !Collections.disjoint(requiredScopes, permission.getScopes());

                LOG.trace("containsAny: " + containsAny + ", requiredScopes: " + requiredScopes + ", permissionScopes: " + permission.getScopes());

                if (containsAny) {
                    if (isGat) { // GAT
                        LOG.debug("GAT has enough permissions, access GRANTED. Path: " + params.getPath() + ", httpMethod:" + params.getHttpMethod() + ", site: " + site);
                        return okResponse(new RsCheckAccessResponse("granted"));
                    }
                    if ((permission.getResourceSetId() != null && permission.getResourceSetId().equals(resource.getId()))) { // normal UMA
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
        final ServerResponse response = (ServerResponse) rptInterceptor.registerTicketResponse(scopes, resource.getId());

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
