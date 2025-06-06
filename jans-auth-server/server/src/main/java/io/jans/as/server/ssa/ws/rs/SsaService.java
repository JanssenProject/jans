/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */
package io.jans.as.server.ssa.ws.rs;

import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.common.model.ssa.SsaState;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.error.ErrorResponseFactory;
import io.jans.as.model.exception.CryptoProviderException;
import io.jans.as.model.exception.InvalidJwtException;
import io.jans.as.model.jwk.KeyOpsType;
import io.jans.as.model.jwk.Use;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.jwt.JwtClaims;
import io.jans.as.model.jwt.JwtType;
import io.jans.as.model.ssa.SsaErrorResponseType;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.model.util.StringUtils;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.commons.collections.CollectionUtils;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static io.jans.as.model.ssa.SsaRequestParam.*;

/**
 * Provides SSA methods to save, update, search, etc.
 */
@Stateless
@Named
public class SsaService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Inject
    private ErrorResponseFactory errorResponseFactory;

    /**
     * Persist SSA in to the database
     *
     * @param ssa New SSA that should be created.
     */
    public void persist(Ssa ssa) {
        persistenceEntryManager.persist(ssa);
    }

    /**
     * Updates an existing SSA in the database
     *
     * @param ssa SSA to be updated.
     */
    public void merge(Ssa ssa) {
        persistenceEntryManager.merge(ssa);
    }

    /**
     * Find SSA based on "jti"
     * <p>
     * Method returns null if the SSA is not found.
     * </p>
     *
     * @param jti Unique identifier
     * @return {@link Ssa} found
     */
    public Ssa findSsaByJti(String jti) {
        try {
            return persistenceEntryManager.find(Ssa.class, getDnForSsa(jti));
        } catch (EntryPersistenceException e) {
            return null;
        }
    }

    /**
     * Get list of SSAs based on "jti", "org_id" or "status" filters
     * <p>
     * If the client only has ssa.portal scope, then it is filtered by the client that created the SSA
     * </p>
     *
     * @param jti      Unique identifier
     * @param orgId    Organization ID
     * @param status   Status
     * @param clientId Client ID
     * @param scopes   List of scope
     * @return List of SSA
     */
    public List<Ssa> getSsaList(String jti, String orgId, SsaState status, String clientId, String[] scopes) {
        List<Filter> filters = new ArrayList<>();
        if (hasDeveloperScope(Arrays.asList(scopes))) {
            filters.add(Filter.createEqualityFilter("creatorId", clientId));
        }
        if (jti != null) {
            filters.add(Filter.createEqualityFilter("inum", jti));
        }
        if (orgId != null) {
            filters.add(Filter.createEqualityFilter("o", orgId));
        }
        if (status != null) {
            filters.add(Filter.createEqualityFilter("jansState", status));
        }
        Filter filter = null;
        if (!filters.isEmpty()) {
            filter = Filter.createANDFilter(filters);
            log.trace("Filter with AND created: " + filters);
        }
        return persistenceEntryManager.findEntries(getDnForSsa(null), Ssa.class, filter);
    }

    /**
     * Generates a new JWT using a given SSA.
     * <p>
     * Method throws an {@link Exception} if it fails to generate JWT
     * </p>
     * <p>
     * Method executes a postProcessor in case it has been sent in the execution context parameter.
     * </p>
     *
     * @param ssa              Ssa
     * @param executionContext Execution context
     * @return Jwt with SSA structure
     */
    public Jwt generateJwt(Ssa ssa, ExecutionContext executionContext) throws Exception {
        Jwt jwt = generateJwt(ssa);
        if (executionContext.getPostProcessor() != null) {
            executionContext.getPostProcessor().apply(jwt);
        }
        return jwt;
    }

    /**
     * Generates a new JWT using a given SSA.
     * <p>
     * Method throws an {@link CryptoProviderException} or {@link InvalidJwtException} if it fails to generate JWT
     * </p>
     *
     * @param ssa Ssa
     * @return Jwt with SSA structure
     */
    public Jwt generateJwt(Ssa ssa) throws CryptoProviderException, InvalidJwtException {
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(appConfiguration.getSsaConfiguration().getSsaSigningAlg());
        if (signatureAlgorithm == null) {
            log.error("Invalid signature algorithm, not found: {}", appConfiguration.getSsaConfiguration().getSsaSigningAlg());
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, SsaErrorResponseType.INVALID_SIGNATURE, "Invalid signature error");
        }
        String keyId = cryptoProvider.getKeyId(webKeysConfiguration, signatureAlgorithm.getAlg(), Use.SIGNATURE, KeyOpsType.SSA);
        if (keyId == null) {
            log.error("Invalid keyId, not found: {}", appConfiguration.getSsaConfiguration().getSsaSigningAlg());
            throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, SsaErrorResponseType.INVALID_SIGNATURE, "Invalid signature error");
        }

        Jwt jwt = new Jwt();
        jwt.getHeader().setType(JwtType.JWT);
        jwt.getHeader().setAlgorithm(signatureAlgorithm);
        jwt.getHeader().setKeyId(keyId);

        fillPayload(jwt.getClaims(), ssa);

        String signature = cryptoProvider.sign(jwt.getSigningInput(), jwt.getHeader().getKeyId(), null, signatureAlgorithm);
        jwt.setEncodedSignature(signature);

        return jwt;
    }

    public void fillPayload(JwtClaims claims, Ssa ssa) {
        claims.setJwtId(ssa.getId());
        claims.setIssuedAt(ssa.getCreationDate());
        claims.setExpirationTime(ssa.getExpirationDate());
        claims.setIssuer(appConfiguration.getIssuer());
        claims.setClaim(SOFTWARE_ID.getName(), ssa.getAttributes().getSoftwareId());
        claims.setClaim(ORG_ID.getName(), ssa.getOrgId());
        claims.setClaim(SOFTWARE_ROLES.getName(), ssa.getAttributes().getSoftwareRoles());
        claims.setClaim(GRANT_TYPES.getName(), ssa.getAttributes().getGrantTypes());
        claims.setClaim(LIFETIME.getName(), ssa.getAttributes().getLifetime());
        if (CollectionUtils.isNotEmpty(ssa.getAttributes().getScopes())) {
            claims.setClaim(SCOPE.getName(), StringUtils.implode(ssa.getAttributes().getScopes(), " "));
        }
        if (!ssa.getAttributes().getCustomAttributes().isEmpty()) {
            ssa.getAttributes().getCustomAttributes().forEach((key, value) -> claims.setClaim(key, value));
        }

        try {
            if (log.isDebugEnabled()) {
                log.debug("Claims: {}", claims.toJsonString());
            }
        } catch (InvalidJwtException e) {
            // ignore, it's just for debug
        }
    }

    /**
     * Create a {@link Response.ResponseBuilder} with status 422
     *
     * @return Response builder
     */
    public Response.ResponseBuilder createUnprocessableEntityResponse() {
        return Response.status(HttpStatus.SC_UNPROCESSABLE_ENTITY).type(MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * Create a {@link Response.ResponseBuilder} with status 406
     *
     * @return Response builder
     */
    public Response.ResponseBuilder createNotAcceptableResponse() {
        return Response.status(HttpStatus.SC_NOT_ACCEPTABLE).type(MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * Check if there is only one "ssa.developer" scope
     *
     * @param scopes List of scope
     * @return true if is only one "ssa.developer", or false otherwise
     */
    private boolean hasDeveloperScope(List<String> scopes) {
        Iterator<String> scopesIterator = scopes.iterator();
        boolean result = false;
        while (scopesIterator.hasNext()) {
            String scope = scopesIterator.next();
            if (scope.equals(SsaScopeType.SSA_ADMIN.getValue()) || scope.equals(SsaScopeType.SSA_PORTAL.getValue())) {
                return false;
            } else if (scope.equals(SsaScopeType.SSA_DEVELOPER.getValue())) {
                result = true;
            }
        }
        return result;
    }

    /**
     * Build a DN for SSA
     *
     * @param ssaId SSA ID
     * @return DN of SSA
     */
    private String getDnForSsa(String ssaId) {
        String baseDn = staticConfiguration.getBaseDn().getSsa();
        if (StringHelper.isEmpty(ssaId)) {
            return baseDn;
        }
        return String.format("inum=%s,%s", ssaId, baseDn);
    }
}
