/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.ssa.ws.rs.action;

import io.jans.as.client.SsaRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.common.service.AttributeService;
import io.jans.as.common.service.common.InumService;
import io.jans.as.model.common.CreatorType;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.model.ssa.SsaRequestParam;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.model.token.JsonWebResponse;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.service.external.ModifySsaResponseService;
import io.jans.as.server.service.external.context.ModifySsaResponseContext;
import io.jans.as.server.ssa.ws.rs.SsaContextBuilder;
import io.jans.as.server.ssa.ws.rs.SsaJsonService;
import io.jans.as.server.ssa.ws.rs.SsaRestWebServiceValidator;
import io.jans.as.server.ssa.ws.rs.SsaService;
import io.jans.as.server.util.ServerUtil;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;

@Stateless
@Named
public class SsaCreateAction {

    @Inject
    private Logger log;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    @Inject
    private InumService inumService;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private SsaJsonService ssaJsonService;

    @Inject
    private SsaService ssaService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private AttributeService attributeService;

    @Inject
    private ModifySsaResponseService modifySsaResponseService;

    @Inject
    private SsaRestWebServiceValidator ssaRestWebServiceValidator;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private SsaContextBuilder ssaContextBuilder;

    public Response create(String requestParams, HttpServletRequest httpRequest, SecurityContext securityContext) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.SSA);
        Response.ResponseBuilder builder = Response.status(Response.Status.CREATED);
        try {
            JSONObject jsonRequest = new JSONObject(requestParams);
            final SsaRequest ssaRequest = SsaRequest.fromJson(jsonRequest);
            log.debug("Attempting to create ssa: {}", ssaRequest);

            String ssaBaseDN = staticConfiguration.getBaseDn().getSsa();
            String inum = inumService.generateDefaultId();
            Client client = ssaRestWebServiceValidator.validateClient();
            ssaRestWebServiceValidator.checkScopesPolicy(client, SsaScopeType.SSA_ADMIN.getValue());

            final Date creationDate = new Date();
            final Date expirationDate = getExpiration(ssaRequest);

            final Ssa ssa = new Ssa();
            ssa.setDn("inum=" + inum + "," + ssaBaseDN);
            ssa.setId(inum);
            ssa.setDeletable(true);
            ssa.setOrgId(ssaRequest.getOrgId() != null ? ssaRequest.getOrgId().toString() : null); // should orgId be long or string? e.g. guid as orgId sounds common
            ssa.setExpirationDate(expirationDate);
            ssa.setTtl(ServerUtil.calculateTtl(creationDate, expirationDate));
            ssa.setDescription(ssaRequest.getDescription());
            ssa.getAttributes().setSoftwareId(ssaRequest.getSoftwareId());
            ssa.getAttributes().setSoftwareRoles(ssaRequest.getSoftwareRoles());
            ssa.getAttributes().setGrantTypes(ssaRequest.getGrantTypes());
            ssa.getAttributes().setCustomAttributes(getCustomAttributes(jsonRequest));
            ssa.getAttributes().setClientDn(client.getDn());
            ssa.getAttributes().setOneTimeUse(ssaRequest.getOneTimeUse());
            ssa.getAttributes().setRotateSsa(ssaRequest.getRotateSsa());
            ssa.setCreatorType(CreatorType.CLIENT);
            ssa.setCreatorId(client.getClientId());

            ssa.setCreationDate(creationDate);
            ssaService.persist(ssa);
            log.info("Ssa created: {}", ssa);

            ModifySsaResponseContext context = ssaContextBuilder.buildModifySsaResponseContext(httpRequest, null, client, appConfiguration, attributeService);
            Function<JsonWebResponse, Void> postProcessor = modifySsaResponseService.buildCreateProcessor(context);
            final ExecutionContext executionContext = context.toExecutionContext();
            executionContext.setPostProcessor(postProcessor);

            Jwt jwt = ssaService.generateJwt(ssa, executionContext, webKeysConfiguration, null);
            JSONObject jsonResponse = ssaJsonService.getJSONObject(jwt.toString());
            builder.entity(ssaJsonService.jsonObjectToString(jsonResponse));

        } catch (WebApplicationException e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            throw e;

        } catch (Exception e) {
            log.error(e.getMessage(), e);
            throw errorResponseFactory.createWebApplicationException(Response.Status.INTERNAL_SERVER_ERROR, SsaErrorResponseType.UNKNOWN_ERROR, "Unknown error");
        }

        builder.cacheControl(ServerUtil.cacheControl(true, false));
        builder.header(Constants.PRAGMA, Constants.NO_CACHE);
        builder.type(MediaType.APPLICATION_JSON_TYPE);
        return builder.build();
    }

    private Map<String, String> getCustomAttributes(JSONObject jsonRequest) {
        if (appConfiguration.getSsaConfiguration().getSsaCustomAttributes().isEmpty())
            return new HashMap<>();

        Map<String, String> customAttributes = new HashMap<>();
        appConfiguration.getSsaConfiguration().getSsaCustomAttributes().forEach(customAttrKey -> {
            if (jsonRequest.has(customAttrKey)) {
                customAttributes.put(customAttrKey, jsonRequest.getString(customAttrKey));
            } else {
                log.warn("Field: {} is not found in request", customAttrKey);
            }
        });

        List<String> ssaFields = new ArrayList<>();
        ssaFields.add(SsaRequestParam.DESCRIPTION.getName());
        ssaFields.add(SsaRequestParam.GRANT_TYPES.getName());
        ssaFields.add(SsaRequestParam.SOFTWARE_ROLES.getName());
        ssaFields.add(SsaRequestParam.ORG_ID.getName());
        ssaFields.add(SsaRequestParam.EXPIRATION.getName());
        ssaFields.add(SsaRequestParam.SOFTWARE_ID.getName());
        ssaFields.add(SsaRequestParam.ONE_TIME_USE.getName());
        ssaFields.add(SsaRequestParam.ROTATE_SSA.getName());
        ssaFields.addAll(appConfiguration.getSsaConfiguration().getSsaCustomAttributes());
        jsonRequest.toMap().forEach((k, v) -> {
            if (!ssaFields.contains(k)) log.warn("Field: {} is not defined", k);
        });

        return customAttributes;
    }

    private Date getExpiration(SsaRequest ssaRequest) {
        Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        if (ssaRequest.getExpiration() != null && ssaRequest.getExpiration() > 0) {
            calendar.setTimeInMillis(ssaRequest.getExpiration() * 1000L);
            return calendar.getTime();
        }
        calendar.add(Calendar.DATE, appConfiguration.getSsaConfiguration().getSsaExpirationInDays());
        return calendar.getTime();
    }
}
