package io.jans.ca.server.op;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import io.jans.as.client.uma.UmaClientFactory;
import io.jans.as.client.uma.UmaResourceService;
import io.jans.as.model.uma.JsonLogicNodeParser;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.as.model.uma.UmaResourceWithId;
import io.jans.ca.common.*;
import io.jans.ca.common.params.RsModifyParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.RsModifyResponse;
import io.jans.ca.rs.protect.resteasy.PatProvider;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.configuration.model.UmaResource;
import io.jans.ca.server.service.*;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class RsModifyOperation extends BaseOperation<RsModifyParams> {

    private static final Logger LOG = LoggerFactory.getLogger(RsModifyOperation.class);

    private UmaTokenService umaTokenService;
    private DiscoveryService discoveryService;
    private RpService rpService;
    private HttpService httpService;

    public RsModifyOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, RsModifyParams.class);
        this.discoveryService = serviceProvider.getDiscoveryService();
        this.umaTokenService = serviceProvider.getUmaTokenService();
        this.httpService = serviceProvider.getHttpService();
        this.rpService = serviceProvider.getRpService();
    }


    @Override
    public IOpResponse execute(final RsModifyParams params) throws Exception {
        validate(params);

        Rp rp = getRp();

        PatProvider patProvider = new PatProvider() {
            @Override
            public String getPatToken() {
                return umaTokenService.getPat(params.getRpId()).getToken();
            }

            @Override
            public void clearPat() {
                // do nothing
            }
        };

        UmaResource umaResource = rp.umaResource(params.getPath(), params.getHttpMethod());
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

        UmaMetadata discovery = discoveryService.getUmaDiscoveryByRpId(params.getRpId());
        UmaResourceService resourceService = UmaClientFactory.instance().createResourceService(discovery, httpService.getClientEngine());

        io.jans.as.model.uma.UmaResource opUmaResource = getResource(resourceService, params, umaResource.getId());

        try {
            String pat = umaTokenService.getPat(params.getRpId()).getToken();
            return update(pat, umaResource.getId(), rp, resourceService, opUmaResource);
        } catch (ClientErrorException e) {
            LOG.debug("Failed to update resource. Entity: " + e.getResponse().readEntity(String.class) + ", status: " + e.getResponse().getStatus(), e);
            if (e.getResponse().getStatus() == 400 || e.getResponse().getStatus() == 401) {
                LOG.debug("Try maybe PAT is lost on AS, force refresh PAT and re-try ...");
                return update(umaTokenService.obtainPat(params.getRpId()).getToken(), umaResource.getId(), rp, resourceService, opUmaResource);
            } else {
                throw e;
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

    public RsModifyResponse update(String pat, String resourceId, Rp rp, UmaResourceService resourceService, io.jans.as.model.uma.UmaResource opUmaResource) {
        resourceService.updateResource("Bearer " + pat, resourceId, opUmaResource);
        updateRp(opUmaResource, rp, resourceId);

        return new RsModifyResponse(rp.getRpId());
    }

    private io.jans.as.model.uma.UmaResource getResource(UmaResourceService resourceService, RsModifyParams params, String resourceId) {
        String pat = umaTokenService.getPat(params.getRpId()).getToken();
        UmaResourceWithId umaResourceWithId = resourceService.getResource("Bearer " + pat, resourceId);

        io.jans.as.model.uma.UmaResource umaResource = new io.jans.as.model.uma.UmaResource();
        umaResource.setDescription(umaResourceWithId.getDescription());
        umaResource.setIat(umaResourceWithId.getIat());
        umaResource.setIconUri(umaResourceWithId.getIconUri());
        umaResource.setName(umaResourceWithId.getName());
        umaResource.setScopes(params.getScopes());
        umaResource.setScopeExpression(null);
        umaResource.setType(umaResourceWithId.getType());
        if (!Strings.isNullOrEmpty(params.getScopeExpression()) && !params.getScopeExpression().equals("null")) {
            umaResource.setScopeExpression(params.getScopeExpression());
            umaResource.setScopes(JsonLogicNodeParser.parseNode(params.getScopeExpression().toString()).getData());
        }

        return umaResource;
    }

    private void updateRp(io.jans.as.model.uma.UmaResource opUmaResource, Rp rp, String resourceId) {
        List<UmaResource> umaResourceList = rp.getUmaProtectedResources();

        rp.setUmaProtectedResources(umaResourceList.stream().map(res -> {
            if (res.getId().equals(resourceId)) {
                res.setScopes(opUmaResource.getScopes());
                res.setTicketScopes(opUmaResource.getScopes());
                res.setScopeExpressions(null);
                if (!Strings.isNullOrEmpty(opUmaResource.getScopeExpression()) && !opUmaResource.getScopeExpression().equals("null")) {
                    res.setScopeExpressions(Lists.newArrayList(opUmaResource.getScopeExpression()));
                    res.setTicketScopes(JsonLogicNodeParser.parseNode(opUmaResource.getScopeExpression().toString()).getData());
                    res.setScopes(null);
                }
            }
            return res;
        }).collect(Collectors.toList()));

        rpService.update(rp);
    }

    private void validate(RsModifyParams params) {

        if (Strings.isNullOrEmpty(params.getRpId())) {
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_RP_ID);
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
                RsProtectOperation.validateScopeExpression(json);
            }
        }
    }
}
