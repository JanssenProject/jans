/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */

package io.jans.as.server.ssa.ws.rs.action;

import io.jans.as.client.ssa.create.SsaCreateRequest;
import io.jans.as.common.model.registration.Client;
import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.common.model.ssa.SsaState;
import io.jans.as.common.service.common.InumService;
import io.jans.as.model.common.CreatorType;
import io.jans.as.model.common.FeatureFlagType;
import io.jans.as.model.config.Constants;
import io.jans.as.model.config.StaticConfiguration;
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
import org.json.JSONObject;
import org.slf4j.Logger;

import java.util.*;
import java.util.function.Function;

/**
 * Provides required methods to create a new SSA considering all required conditions.
 */
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
    private ModifySsaResponseService modifySsaResponseService;

    @Inject
    private SsaRestWebServiceValidator ssaRestWebServiceValidator;

    @Inject
    private SsaContextBuilder ssaContextBuilder;

    /**
     * Creates an SSA from the requested parameters.
     * <p>
     * Method will return a {@link WebApplicationException} with status {@code 401} if this functionality is not enabled,
     * request has to have at least scope "ssa.admin",
     * it will also return a {@link WebApplicationException} with status {@code 500} in case an uncontrolled
     * error occurs when processing the method.
     * <p/>
     * <p>
     * Response of this method can be modified using the following custom script
     * <a href="https://github.com/JanssenProject/jans/blob/main/jans-linux-setup/jans_setup/static/extension/ssa_modify_response/ssa_modify_response.py">SSA Custom Script</a>,
     * method create.
     * </p>
     * <p>
     * SSA returned by this method is stored in the corresponding database, so it can be later retrieved, validated or revoked.
     * <p/>
     *
     * @param requestParams Valid json request
     * @param httpRequest   Http request
     * @return {@link Response} with status {@code 201} (Created) and response body containing the SSA in JWT format.
     */
    public Response create(String requestParams, HttpServletRequest httpRequest) {
        errorResponseFactory.validateFeatureEnabled(FeatureFlagType.SSA);
        Response.ResponseBuilder builder = Response.status(Response.Status.CREATED);
        try {
            JSONObject jsonRequest = new JSONObject(requestParams);
            final SsaCreateRequest ssaCreateRequest = SsaCreateRequest.fromJson(jsonRequest);
            log.debug("Attempting to create ssa: {}", ssaCreateRequest);
            log.trace("Ssa request = {}", requestParams);

            String ssaBaseDN = staticConfiguration.getBaseDn().getSsa();
            String inum = inumService.generateDefaultId();
            Client client = ssaRestWebServiceValidator.getClientFromSession();
            ssaRestWebServiceValidator.checkScopesPolicy(client, SsaScopeType.SSA_ADMIN.getValue());

            final Date creationDate = new Date();
            final Date expirationDate = getExpiration(ssaCreateRequest);

            final Ssa ssa = new Ssa();
            ssa.setDn("inum=" + inum + "," + ssaBaseDN);
            ssa.setId(inum);
            ssa.setDeletable(true);
            ssa.setOrgId(ssaCreateRequest.getOrgId());
            ssa.setExpirationDate(expirationDate);
            ssa.setTtl(ServerUtil.calculateTtl(creationDate, expirationDate));
            ssa.setDescription(ssaCreateRequest.getDescription());
            ssa.getAttributes().setSoftwareId(ssaCreateRequest.getSoftwareId());
            ssa.getAttributes().setSoftwareRoles(ssaCreateRequest.getSoftwareRoles());
            ssa.getAttributes().setGrantTypes(ssaCreateRequest.getGrantTypes());
            ssa.getAttributes().setCustomAttributes(getCustomAttributes(jsonRequest));
            ssa.getAttributes().setClientDn(client.getDn());
            ssa.getAttributes().setOneTimeUse(ssaCreateRequest.getOneTimeUse());
            ssa.getAttributes().setRotateSsa(ssaCreateRequest.getRotateSsa());
            ssa.setCreatorType(CreatorType.CLIENT);
            ssa.setState(SsaState.ACTIVE);
            ssa.setCreatorId(client.getClientId());
            ssa.setCreationDate(creationDate);

            ModifySsaResponseContext context = ssaContextBuilder.buildModifySsaResponseContext(httpRequest, client);
            Function<JsonWebResponse, Void> postProcessor = modifySsaResponseService.buildCreateProcessor(context);
            final ExecutionContext executionContext = context.toExecutionContext();
            executionContext.setPostProcessor(postProcessor);

            Jwt jwt = ssaService.generateJwt(ssa, executionContext);
            ssaService.persist(ssa);
            log.info("Ssa created: {}", ssa);
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

    /**
     * Get custom attributes from a request, previously configured in SSA global parameters.
     * <p>
     * Method prints the warning type in case a request parameter does not exist in the SSA model or SSA global parameter settings.
     * </p>
     *
     * @param jsonRequest Valid json request
     * @return Map containing all custom attributes where key is the attribute name.
     */
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

    /**
     * Get request expiration date using UTC timezone.
     * <p>
     * Method converts from epoch time to Date or generates a date based on the global SSA setting when the Date field
     * of the request is null.
     * </p>
     *
     * @param ssaCreateRequest Request of SSA
     * @return Respective new Date instance.
     */
    private Date getExpiration(SsaCreateRequest ssaCreateRequest) {
        Calendar calendar = GregorianCalendar.getInstance(TimeZone.getTimeZone("UTC"));
        if (ssaCreateRequest.getExpiration() != null && ssaCreateRequest.getExpiration() > 0) {
            calendar.setTimeInMillis(ssaCreateRequest.getExpiration() * 1000L);
            return calendar.getTime();
        }
        calendar.add(Calendar.DATE, appConfiguration.getSsaConfiguration().getSsaExpirationInDays());
        return calendar.getTime();
    }
}
