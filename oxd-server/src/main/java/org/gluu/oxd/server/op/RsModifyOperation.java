package org.gluu.oxd.server.op;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.gluu.oxauth.client.uma.UmaClientFactory;
import org.gluu.oxauth.client.uma.UmaResourceService;
import org.gluu.oxauth.model.uma.*;
import org.gluu.oxauth.model.util.Util;
import org.gluu.oxd.common.*;
import org.gluu.oxd.common.params.RsModifyParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.RsModifyResponse;
import org.gluu.oxd.rs.protect.resteasy.PatProvider;
import org.gluu.oxd.server.HttpException;
import org.gluu.oxd.server.service.Rp;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class RsModifyOperation extends BaseOperation<RsModifyParams> {

    private static final Logger LOG = LoggerFactory.getLogger(RsModifyOperation.class);

    /**
     * Constructor
     *
     * @param command command
     */
    protected RsModifyOperation(Command command, final Injector injector) {
        super(command, injector, RsModifyParams.class);
    }


    @Override
    public IOpResponse execute(final RsModifyParams params) throws Exception {
        validate(params);

        Rp site = getRp();

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

        org.gluu.oxd.server.model.UmaResource umaResource = site.umaResource(params.getPath(), params.getHttpMethod());
        if (umaResource == null) {
            final ErrorResponse error = new ErrorResponse("invalid_request");
            error.setErrorDescription("Resource is not protected with path: " + params.getPath() + " and httpMethod: " + params.getHttpMethod() +
                    ". Please protect your resource first with uma_rs_modify command. Check details on " + CoreUtils.DOC_URL);
            LOG.error(error.getErrorDescription());
            throw new WebApplicationException(Response
                    .status(Response.Status.BAD_REQUEST)
                    .type(MediaType.APPLICATION_JSON_TYPE)
                    .entity(Jackson2.asJson(error))
                    .build());
        }

        UmaMetadata discovery = getDiscoveryService().getUmaDiscoveryByOxdId(params.getOxdId());
        UmaResourceService resourceService = UmaClientFactory.instance().createResourceService(discovery, getHttpService().getClientExecutor());

        UmaResource opUmaResource = getResource(resourceService, params, umaResource.getId());
        setResource(umaResource, opUmaResource);

        try {
            String pat = getUmaTokenService().getPat(params.getOxdId()).getToken();
            resourceService.updateResource("Bearer " + pat, umaResource.getId(), opUmaResource);
        } catch (ClientResponseFailure e) {
            LOG.debug("Failed to update resource. Entity: " + e.getResponse().getEntity(String.class) + ", status: " + e.getResponse().getStatus(), e);
            if (e.getResponse().getStatus() == 400 || e.getResponse().getStatus() == 401) {
                LOG.debug("Try maybe PAT is lost on AS, force refresh PAT and re-try ...");
            }
            throw e;
        }

        update(umaResource, site);

        return new RsModifyResponse(site.getOxdId());
    }

    private void setResource(org.gluu.oxd.server.model.UmaResource umaResource, UmaResource opUmaResource) {
        umaResource.setScopes(opUmaResource.getScopes());
        if (!Strings.isNullOrEmpty(opUmaResource.getScopeExpression()) && !opUmaResource.getScopeExpression().equals("null")) {
            umaResource.setScopeExpressions(Lists.newArrayList());
            umaResource.getScopeExpressions().add(opUmaResource.getScopeExpression());
        }
    }

    private UmaResource getResource(UmaResourceService resourceService, RsModifyParams params, String resourceId) {
        String pat = getUmaTokenService().getPat(params.getOxdId()).getToken();
        UmaResourceWithId umaResourceWithId = resourceService.getResource("Bearer " + pat, resourceId);

        UmaResource umaResource = new UmaResource();
        umaResource.setDescription(umaResourceWithId.getDescription());
        umaResource.setIat(umaResourceWithId.getIat());
        umaResource.setIconUri(umaResourceWithId.getIconUri());
        umaResource.setName(umaResourceWithId.getName());
        umaResource.setScopes(params.getScopes());
        umaResource.setType(umaResourceWithId.getType());
        if (!Strings.isNullOrEmpty(params.getScopeExpression()) && !params.getScopeExpression().equals("null")) {
            umaResource.setScopeExpression(params.getScopeExpression());
        }

        return umaResource;
    }

    private void update(org.gluu.oxd.server.model.UmaResource resource, Rp site) throws IOException {
        List<org.gluu.oxd.server.model.UmaResource> umaResourceList = site.getUmaProtectedResources();

        site.setUmaProtectedResources(umaResourceList.stream().map(res -> res.getId().equals(resource.getId()) ? resource : res).collect(Collectors.toList()));

        getRpService().update(site);
    }

    private void validate(RsModifyParams params) {

        if (Strings.isNullOrEmpty(params.getOxdId())) {
            throw new HttpException(ErrorResponseCode.NO_UMA_OXD_ID_PARAMETER);
        }

        if (Strings.isNullOrEmpty(params.getHttpMethod())) {
            throw new HttpException(ErrorResponseCode.NO_UMA_HTTP_METHOD);
        }

        if (Strings.isNullOrEmpty(params.getPath())) {
            throw new HttpException(ErrorResponseCode.NO_UMA_PATH_PARAMETER);
        }

        if (!Strings.isNullOrEmpty(params.getScopeExpression())) {
            String json = params.getScopeExpression();
            if (StringUtils.isNotBlank(json) && !json.equalsIgnoreCase("null")) {
                boolean nodeValid = JsonLogicNodeParser.isNodeValid(json);
                LOG.trace("Scope expression validator - Valid: " + nodeValid + ", expression: " + json);
                if (!nodeValid) {
                    throw new HttpException(ErrorResponseCode.UMA_FAILED_TO_VALIDATE_SCOPE_EXPRESSION);
                }
                validateScopeExpression(json);
            }
        }
    }

    public static void validateScopeExpression(String scopeExpression) {
        JsonLogicNode jsonLogicNode = JsonLogicNodeParser.parseNode(scopeExpression);
        try {
            Object scope = JsonLogic.applyObject(jsonLogicNode.getRule().toString(), Util.asJsonSilently(jsonLogicNode.getData()));
            if (scope == null || !jsonLogicNode.getData().contains(scope.toString())) {
                throw new HttpException(ErrorResponseCode.UMA_FAILED_TO_VALIDATE_SCOPE_EXPRESSION);
            }
        } catch (Exception e) {
            LOG.trace("The scope expression is invalid. Please check the documentation and make sure it is a valid JsonLogic expression.", e);
            throw new HttpException(ErrorResponseCode.UMA_FAILED_TO_VALIDATE_SCOPE_EXPRESSION);
        }
    }
}
