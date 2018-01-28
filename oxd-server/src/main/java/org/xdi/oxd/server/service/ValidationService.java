package org.xdi.oxd.server.service;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.jboss.resteasy.client.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.service.IntrospectionService;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.*;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.ServerLauncher;
import org.xdi.oxd.server.license.LicenseService;

/**
 * @author Yuriy Zabrovarnyy
 */

public class ValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationService.class);

    public void notNull(IParams params) {
        if (params == null) {
            throw new ErrorResponseException(ErrorResponseCode.INTERNAL_ERROR_NO_PARAMS);
        }
    }

    public void notBlankOxdId(String oxdId) {
        if (Strings.isNullOrEmpty(oxdId)) {
            throw new ErrorResponseException(ErrorResponseCode.BAD_REQUEST_NO_OXD_ID);
        }
    }

    public void notBlankOpHost(String opHost) {
        if (Strings.isNullOrEmpty(opHost)) {
            throw new ErrorResponseException(ErrorResponseCode.INVALID_OP_HOST);
        }
    }

    public void validate(IParams params) {
        Boolean isClientLocal = null;
        notNull(params);
        if (params instanceof HasOxdIdParams) {
            validate((HasOxdIdParams) params);
            isClientLocal = true;
        }
        if (params instanceof HasProtectionAccessTokenParams) {
            if (validate((HasProtectionAccessTokenParams) params)) {
                isClientLocal = false;
            }
        }

        if (isClientLocal != null && !(params instanceof RegisterSiteParams)) {
            try {
                String oxdId = ((HasOxdIdParams) params).getOxdId();
                if (StringUtils.isNotBlank(oxdId)) {
                    final RpService rpService = ServerLauncher.getInjector().getInstance(RpService.class);
                    final Rp rp = rpService.getRp(oxdId);
                    if (rp != null) {
                        ServerLauncher.getInjector().getInstance(LicenseService.class).notifyClientUsed(rp, isClientLocal);
                    }
                }
            } catch (ErrorResponseException e) {
                // ignore
            } catch (Exception e) {
                LOG.error("Failed to invoke license service client update. Message: " + e.getMessage(), e);
            }
        }
    }

    /**
     * Returns whether has valid token
     *
     * @param params params
     * @return true - client is remote, false - client is local. If validation does not pass exception must be thrown
     */
    private boolean validate(HasProtectionAccessTokenParams params) {
        if (params instanceof SetupClientParams) {
            return false;
        }
        if (params instanceof UpdateSiteParams) {
            final RpService rpService = ServerLauncher.getInjector().getInstance(RpService.class);
            final Rp rp = rpService.getRp(params.getOxdId());
            if (rp.getSetupClient() != null && rp.getSetupClient()) {
                return false; // skip validation if client is setup client (if we can setup client without protection access token then we allow also update it)
            }
        }

        final Configuration configuration = ServerLauncher.getInjector().getInstance(ConfigurationService.class).get();
        if (configuration.getProtectCommandsWithAccessToken() != null && !configuration.getProtectCommandsWithAccessToken()) {
            if (StringUtils.isBlank(params.getProtectionAccessToken())) {
                return false; // skip validation since protectCommandsWithAccessToken=false
            } // otherwise if token is not blank then let it validate it
        }

        final String accessToken = params.getProtectionAccessToken();

        if (StringUtils.isBlank(accessToken)) {
            throw new ErrorResponseException(ErrorResponseCode.BLANK_PROTECTION_ACCESS_TOKEN);
        }
        if (params instanceof RegisterSiteParams) {
            return false; // skip validation for site registration because we have to associate oxd_id with client_id, validation is performed inside operation
        }

        final RpService rpService = ServerLauncher.getInjector().getInstance(RpService.class);

        final Rp rp = rpService.getRp(params.getOxdId());
        if (StringUtils.isBlank(rp.getSetupClientId())) {
            throw new ErrorResponseException(ErrorResponseCode.NO_SETUP_CLIENT_FOR_OXD_ID);
        }

        final IntrospectionResponse introspectionResponse = introspect(accessToken, params.getOxdId());

        LOG.trace("access_token: " + accessToken + ", introspection: " + introspectionResponse + ", setupClientId: " + rp.getSetupClientId());
        if (StringUtils.isBlank(introspectionResponse.getClientId())) {
            throw new ErrorResponseException(ErrorResponseCode.NO_CLIENT_ID_IN_INTROSPECTION_RESPONSE);
        }

        if (introspectionResponse.getClientId().equals(rp.getSetupClientId())) {
            return true;
        }

        throw new ErrorResponseException(ErrorResponseCode.INVALID_PROTECTION_ACCESS_TOKEN);
    }

    public IntrospectionResponse introspect(String accessToken, String oxdId) {
        if (StringUtils.isBlank(accessToken)) {
            throw new ErrorResponseException(ErrorResponseCode.BLANK_PROTECTION_ACCESS_TOKEN);
        }

        final RpService rpService = ServerLauncher.getInjector().getInstance(RpService.class);
        final Rp rp = rpService.getRp(oxdId);
        if (StringUtils.isNotBlank(rp.getSetupOxdId())) {
            oxdId = rp.getSetupOxdId();
        }
        LOG.trace("Introspect token with rp: " + rpService.getRp(oxdId));

        final DiscoveryService discoveryService = ServerLauncher.getInjector().getInstance(DiscoveryService.class);
        final String introspectionEndpoint = discoveryService.getConnectDiscoveryResponseByOxdId(oxdId).getIntrospectionEndpoint();
        final UmaTokenService umaTokenService = ServerLauncher.getInjector().getInstance(UmaTokenService.class);
        final HttpService httpService = ServerLauncher.getInjector().getInstance(HttpService.class);
        final IntrospectionService introspectionService = ProxyFactory.create(IntrospectionService.class, introspectionEndpoint, httpService.getClientExecutor());
        IntrospectionResponse response = null;

        try {
            response = introspectionService.introspectToken("Bearer " + umaTokenService.getPat(oxdId).getToken(), accessToken);
        } catch (ClientResponseFailure e) {
            int status = e.getResponse().getStatus();
            LOG.debug("Failed to introspect token. Entity: " + e.getResponse().getEntity(String.class) + ", status: " + status, e);
            if (status == 400 || status == 401) {
                LOG.debug("Try maybe PAT is lost on AS, force refresh PAT and re-try ...");
                umaTokenService.obtainPat(oxdId); // force to refresh PAT
                response = introspectionService.introspectToken("Bearer " + umaTokenService.getPat(oxdId).getToken(), accessToken);
            } else {
                throw e;
            }
        }

        if (!response.isActive()) {
            LOG.debug("access_token is not active.");
            throw new ErrorResponseException(ErrorResponseCode.INACTIVE_PROTECTION_ACCESS_TOKEN);
        }
        return response;
    }

    public void validate(HasOxdIdParams params) {
        notNull(params);
        notBlankOxdId(params.getOxdId());
    }

    public Rp validate(Rp rp) {
        if (rp == null) {
            throw new ErrorResponseException(ErrorResponseCode.INVALID_OXD_ID);
        }

        notBlankOxdId(rp.getOxdId());
        notBlankOpHost(rp.getOpHost());
        return rp;
    }
}
