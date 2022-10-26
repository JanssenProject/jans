/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2022, Janssen Project
 */
package io.jans.as.server.ssa.ws.rs;

import io.jans.as.common.model.ssa.Ssa;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.orm.PersistenceEntryManager;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.slf4j.Logger;

@Stateless
@Named
public class SsaService {

    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager persistenceEntryManager;

    @Inject
    private AppConfiguration appConfiguration;

    public void persist(Ssa ssa) {
        persistenceEntryManager.persist(ssa);
    }

    public void merge(Ssa ssa) {
        persistenceEntryManager.merge(ssa);
    }

    public Jwt generateJwt(Ssa ssa, ExecutionContext executionContext, WebKeysConfiguration webKeysConfiguration, AbstractCryptoProvider cryptoProvider) {
        try {
            SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(appConfiguration.getSsaConfiguration().getSsaSigningAlg());
            JwtSigner jwtSigner = new JwtSigner(appConfiguration, webKeysConfiguration, signatureAlgorithm, null, null, cryptoProvider);
            Jwt jwt = jwtSigner.newJwt();
            jwt.getClaims().setJwtId(ssa.getId());
            jwt.getClaims().setIssuedAt(ssa.getCreationDate());
            jwt.getClaims().setExpirationTime(ssa.getExpirationDate());
            jwt.getClaims().setClaim("software_id", ssa.getAttributes().getSoftwareId());
            jwt.getClaims().setClaim("org_id", Long.parseLong(ssa.getOrgId()));
            jwt.getClaims().setClaim("software_roles", ssa.getAttributes().getSoftwareRoles());
            jwt.getClaims().setClaim("grant_types", ssa.getAttributes().getGrantTypes());

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
}
