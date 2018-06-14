package org.xdi.oxd.server.service;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.*;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.ServerLauncher;
import org.xdi.util.Pair;

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

    public Pair<Rp, Boolean> validate(IParams params) {
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
                        return new Pair<>(rp, isClientLocal);
                    }
                }
            } catch (ErrorResponseException e) {
                if (e.getErrorResponseCode() == ErrorResponseCode.EXPIRED_CLIENT) {
                    throw e;
                }
                // ignore
            } catch (Exception e) {
                LOG.error("Failed to invoke license service client update. Message: " + e.getMessage(), e);
            }
        }
        if (params instanceof GetClientTokenParams) {
            GetClientTokenParams p = (GetClientTokenParams) params;
            String clientId = p.getClientId();
            final RpService rpService = ServerLauncher.getInjector().getInstance(RpService.class);
            Rp rp = rpService.getRpByClientId(clientId);
            if (rp != null) {
                return new Pair<>(rp, false);
            }
        }
        return null;
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

        final IntrospectionService introspectionService = ServerLauncher.getInjector().getInstance(IntrospectionService.class);
        final IntrospectionResponse response = introspectionService.introspectToken(oxdId, accessToken);

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
