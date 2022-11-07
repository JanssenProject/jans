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
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.ssa.SsaScopeType;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.exception.EntryPersistenceException;
import io.jans.orm.search.filter.Filter;
import io.jans.util.StringHelper;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.http.HttpStatus;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import static io.jans.as.model.ssa.SsaRequestParam.*;

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

    public void persist(Ssa ssa) {
        persistenceEntryManager.persist(ssa);
    }

    public void merge(Ssa ssa) {
        persistenceEntryManager.merge(ssa);
    }

    public Ssa findSsaByJti(String jti) {
        try {
            return persistenceEntryManager.find(Ssa.class, getDnForSsa(jti));
        } catch (EntryPersistenceException e) {
            return null;
        }
    }

    public List<Ssa> getSsaList(String jti, Long orgId, SsaState status, String clientId, String[] scopes) {
        List<Filter> filters = new ArrayList<>();
        if (hasPortalScope(Arrays.asList(scopes))) {
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

    public Jwt generateJwt(Ssa ssa, ExecutionContext executionContext, WebKeysConfiguration webKeysConfiguration, AbstractCryptoProvider cryptoProvider) {
        try {
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(appConfiguration.getSsaConfiguration().getSsaSigningAlg());
            JwtSigner jwtSigner = new JwtSigner(appConfiguration, webKeysConfiguration, signatureAlgorithm, null, null, cryptoProvider);
            Jwt jwt = jwtSigner.newJwt();
            jwt.getClaims().setJwtId(ssa.getId());
            jwt.getClaims().setIssuedAt(ssa.getCreationDate());
            jwt.getClaims().setExpirationTime(ssa.getExpirationDate());
            jwt.getClaims().setClaim(SOFTWARE_ID.getName(), ssa.getAttributes().getSoftwareId());
            jwt.getClaims().setClaim(ORG_ID.getName(), Long.parseLong(ssa.getOrgId()));
            jwt.getClaims().setClaim(SOFTWARE_ROLES.getName(), ssa.getAttributes().getSoftwareRoles());
            jwt.getClaims().setClaim(GRANT_TYPES.getName(), ssa.getAttributes().getGrantTypes());

            Jwt jwr = jwtSigner.sign();
            if (executionContext.getPostProcessor() != null) {
                executionContext.getPostProcessor().apply(jwr);
            }
            return jwr;
        } catch (Exception e) {
            if (log.isErrorEnabled())
                log.error("Failed to sign session jwt! " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    public Response.ResponseBuilder createUnprocessableEntityResponse() {
        return Response.status(HttpStatus.SC_UNPROCESSABLE_ENTITY).type(MediaType.APPLICATION_JSON_TYPE);
    }

    public Response.ResponseBuilder createNotAcceptableResponse() {
        return Response.status(HttpStatus.SC_NOT_ACCEPTABLE).type(MediaType.APPLICATION_JSON_TYPE);
    }

    private boolean hasPortalScope(List<String> scopes) {
        Iterator<String> scopesIterator = scopes.iterator();
        boolean result = false;
        while (scopesIterator.hasNext()) {
            String scope = scopesIterator.next();
            if (scope.equals(SsaScopeType.SSA_ADMIN.getValue())) {
                return false;
            } else if (scope.equals(SsaScopeType.SSA_PORTAL.getValue())) {
                result = true;
            }
        }
        return result;
    }

    private String getDnForSsa(String ssaId) {
        String baseDn = staticConfiguration.getBaseDn().getSsa();
        if (StringHelper.isEmpty(ssaId)) {
            return baseDn;
        }
        return String.format("inum=%s,%s", ssaId, baseDn);
    }
}
