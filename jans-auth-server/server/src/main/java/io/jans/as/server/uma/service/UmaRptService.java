/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.uma.service;

import com.google.common.base.Preconditions;
import io.jans.as.common.claims.Audience;
import io.jans.as.common.model.registration.Client;
import io.jans.as.model.common.GrantType;
import io.jans.as.model.config.StaticConfiguration;
import io.jans.as.model.config.WebKeysConfiguration;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.crypto.signature.SignatureAlgorithm;
import io.jans.as.model.jwt.Jwt;
import io.jans.as.model.uma.persistence.UmaPermission;
import io.jans.as.model.util.JwtUtil;
import io.jans.as.server.model.common.ExecutionContext;
import io.jans.as.server.model.token.JwtSigner;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.external.ExternalUmaRptClaimsService;
import io.jans.as.server.service.external.context.ExternalUmaRptClaimsContext;
import io.jans.as.server.service.stat.StatService;
import io.jans.as.server.uma.authorization.UmaPCT;
import io.jans.as.server.uma.authorization.UmaRPT;
import io.jans.as.server.util.ServerUtil;
import io.jans.as.server.util.TokenHashUtil;
import io.jans.orm.PersistenceEntryManager;
import io.jans.orm.model.base.SimpleBranch;
import io.jans.util.INumGenerator;
import io.jans.util.StringHelper;
import org.apache.commons.lang.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * RPT manager component
 *
 * @author Yuriy Zabrovarnyy
 * @author Javier Rojas Blum
 * @version June 28, 2017
 */
@ApplicationScoped
public class UmaRptService {

    public static final int DEFAULT_RPT_LIFETIME = 3600;
    private static final String ORGUNIT_OF_RPT = "uma_rpt";
    @Inject
    private Logger log;

    @Inject
    private PersistenceEntryManager ldapEntryManager;

    @Inject
    private WebKeysConfiguration webKeysConfiguration;

    @Inject
    private UmaPctService pctService;

    @Inject
    private UmaScopeService umaScopeService;

    @Inject
    private AppConfiguration appConfiguration;

    @Inject
    private StaticConfiguration staticConfiguration;

    @Inject
    private ClientService clientService;

    @Inject
    private ExternalUmaRptClaimsService externalUmaRptClaimsService;

    @Inject
    private StatService statService;

    private boolean containsBranch = false;

    public static List<String> getPermissionDns(Collection<UmaPermission> permissions) {
        final List<String> result = new ArrayList<>();
        if (permissions != null) {
            for (UmaPermission p : permissions) {
                result.add(p.getDn());
            }
        }
        return result;
    }

    public String createDn(String tokenCode) {
        return String.format("tknCde=%s,%s", TokenHashUtil.hash(tokenCode), branchDn());
    }

    public String branchDn() {
        return String.format("ou=%s,%s", ORGUNIT_OF_RPT, staticConfiguration.getBaseDn().getTokens());
    }

    public void persist(UmaRPT rpt) {
        try {
            Preconditions.checkNotNull(rpt.getClientId());

            addBranchIfNeeded();
            rpt.setDn(createDn(rpt.getNotHashedCode()));
            rpt.setCode(TokenHashUtil.hash(rpt.getNotHashedCode()));
            ldapEntryManager.persist(rpt);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public UmaRPT getRPTByCode(String rptCode) {
        try {
            final UmaRPT entry = ldapEntryManager.find(UmaRPT.class, createDn(rptCode));
            if (entry != null) {
                return entry;
            } else {
                log.error("Failed to find RPT by code: {}", rptCode);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return null;
    }

    public void deleteByCode(String rptCode) {
        try {
            final UmaRPT t = getRPTByCode(rptCode);
            if (t != null) {
                ldapEntryManager.remove(t);
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public boolean addPermissionToRPT(UmaRPT rpt, Collection<UmaPermission> permissions) {
        return addPermissionToRPT(rpt, permissions.toArray(new UmaPermission[permissions.size()]));
    }

    public boolean addPermissionToRPT(UmaRPT rpt, UmaPermission... permission) {
        if (ArrayUtils.isEmpty(permission)) {
            return true;
        }

        final List<String> permissions = getPermissionDns(Arrays.asList(permission));
        if (rpt.getPermissions() != null) {
            permissions.addAll(rpt.getPermissions());
        }

        rpt.setPermissions(permissions);

        try {
            rpt.resetTtlFromExpirationDate();
            ldapEntryManager.merge(rpt);
            log.trace("Persisted RPT: {}", rpt);
            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    public List<UmaPermission> getRptPermissions(UmaRPT rpt) {
        final List<UmaPermission> result = new ArrayList<>();
        try {
            if (rpt != null && rpt.getPermissions() != null) {
                final List<String> permissionDns = rpt.getPermissions();
                for (String permissionDn : permissionDns) {
                    final UmaPermission permissionObject = ldapEntryManager.find(UmaPermission.class, permissionDn);
                    if (permissionObject != null) {
                        result.add(permissionObject);
                    }
                }
            }
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
        return result;
    }

    public Date rptExpirationDate() {
        int lifeTime = appConfiguration.getUmaRptLifetime();
        if (lifeTime <= 0) {
            lifeTime = DEFAULT_RPT_LIFETIME;
        }

        final Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.SECOND, lifeTime);
        return calendar.getTime();
    }

    public UmaRPT createRPTAndPersist(ExecutionContext executionContext, List<UmaPermission> permissions) {
        try {
            final Date creationDate = new Date();
            final Date expirationDate = rptExpirationDate();
            final Client client = executionContext.getClient();

            final String code;
            if (client.isRptAsJwt()) {
                code = createRptJwt(executionContext, permissions, creationDate, expirationDate);
            } else {
                code = UUID.randomUUID().toString() + "_" + INumGenerator.generate(8);
            }

            UmaRPT rpt = new UmaRPT(code, creationDate, expirationDate, null, client.getClientId());
            rpt.setPermissions(getPermissionDns(permissions));
            persist(rpt);
            statService.reportUmaToken(GrantType.OXAUTH_UMA_TICKET);
            return rpt;
        } catch (Exception e) {
            if (log.isErrorEnabled()) {
                log.error(e.getMessage(), e);
            }
            throw new RuntimeException("Failed to generate RPT, clientId: " + executionContext.getClient().getClientId(), e);
        }
    }

    public void merge(UmaRPT rpt) {
        rpt.resetTtlFromExpirationDate();
        ldapEntryManager.merge(rpt);
    }

    private String createRptJwt(ExecutionContext executionContext, List<UmaPermission> permissions, Date creationDate, Date expirationDate) throws Exception {
        Client client = executionContext.getClient();
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.fromString(appConfiguration.getDefaultSignatureAlgorithm());
        if (client.getAccessTokenSigningAlg() != null && SignatureAlgorithm.fromString(client.getAccessTokenSigningAlg()) != null) {
            signatureAlgorithm = SignatureAlgorithm.fromString(client.getAccessTokenSigningAlg());
        }

        final JwtSigner jwtSigner = new JwtSigner(appConfiguration, webKeysConfiguration, signatureAlgorithm, client.getClientId(), clientService.decryptSecret(client.getClientSecret()));
        final Jwt jwt = jwtSigner.newJwt();
        jwt.getClaims().setClaim("client_id", client.getClientId());
        jwt.getClaims().setExpirationTime(expirationDate);
        jwt.getClaims().setIssuedAt(creationDate);
        Audience.setAudience(jwt.getClaims(), client);

        if (permissions != null && !permissions.isEmpty()) {
            String pctCode = permissions.iterator().next().getAttributes().get(UmaPermission.PCT);
            if (StringHelper.isNotEmpty(pctCode)) {
                UmaPCT pct = pctService.getByCode(pctCode);
                if (pct != null) {
                    jwt.getClaims().setClaim("pct_claims", pct.getClaims().toJsonObject());
                } else {
                    log.error("Failed to find PCT with code: {} which is taken from permission object: {}", pctCode, permissions.iterator().next().getDn());
                }
            }

            jwt.getClaims().setClaim("permissions", buildPermissionsJSONObject(permissions));
        }
        runScriptAndInjectValuesIntoJwt(jwt, executionContext);

        return jwtSigner.sign().toString();
    }

    private void runScriptAndInjectValuesIntoJwt(Jwt jwt, ExecutionContext executionContext) {
        JSONObject responseAsJsonObject = new JSONObject();

        ExternalUmaRptClaimsContext context = new ExternalUmaRptClaimsContext(executionContext);
        if (externalUmaRptClaimsService.externalModify(responseAsJsonObject, context)) {
            log.trace("Successfully run external RPT Claim scripts.");

            if (context.isTranferPropertiesIntoJwtClaims()) {
                log.trace("Transfering claims into jwt ...");
                JwtUtil.transferIntoJwtClaims(responseAsJsonObject, jwt);
                log.trace("Transfered.");
            }
        }
    }

    public JSONArray buildPermissionsJSONObject(List<UmaPermission> permissions) throws IOException, JSONException {
        List<io.jans.as.model.uma.UmaPermission> result = new ArrayList<>();

        for (UmaPermission permission : permissions) {
            permission.checkExpired();
            permission.isValid();
            if (permission.isValid()) {
                final io.jans.as.model.uma.UmaPermission toAdd = ServerUtil.convert(permission, umaScopeService);
                if (toAdd != null) {
                    result.add(toAdd);
                }
            } else {
                log.debug("Ignore permission, skip it in response because permission is not valid. Permission dn: {}", permission.getDn());
            }
        }

        final String json = ServerUtil.asJson(result);
        return new JSONArray(json);
    }

    public void addBranch() {
        final SimpleBranch branch = new SimpleBranch();
        branch.setOrganizationalUnitName(ORGUNIT_OF_RPT);
        branch.setDn(branchDn());
        ldapEntryManager.persist(branch);
    }

    public void addBranchIfNeeded() {
        if (ldapEntryManager.hasBranchesSupport(branchDn())) {
            if (!containsBranch() && !containsBranch) {
                addBranch();
            } else {
                containsBranch = true;
            }
        }
    }

    public boolean containsBranch() {
        return ldapEntryManager.contains(branchDn(), SimpleBranch.class);
    }
}
