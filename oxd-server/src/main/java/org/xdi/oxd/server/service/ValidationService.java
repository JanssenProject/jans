package org.xdi.oxd.server.service;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.params.*;
import org.xdi.oxd.server.HttpException;
import org.xdi.oxd.server.OxdServerConfiguration;
import org.xdi.oxd.server.ServerLauncher;
import org.xdi.util.Pair;

/**
 * @author Yuriy Zabrovarnyy
 */

public class ValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationService.class);

    private void notNull(IParams params) {
        if (params == null) {
            throw new HttpException(ErrorResponseCode.INTERNAL_ERROR_NO_PARAMS);
        }
    }

    public void notBlankOxdId(String oxdId) {
        if (Strings.isNullOrEmpty(oxdId)) {
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_OXD_ID);
        }
    }

    public void notBlankOpHost(String opHost) {
        if (Strings.isNullOrEmpty(opHost)) {
            throw new HttpException(ErrorResponseCode.INVALID_OP_HOST);
        }
    }

    public Pair<Rp, Boolean> validate(IParams params) {
        notNull(params);
        if (params instanceof HasOxdIdParams) {
            validate((HasOxdIdParams) params);
        }
        if (params instanceof HasProtectionAccessTokenParams) {
            validate((HasProtectionAccessTokenParams) params);
        }

        if (!(params instanceof RegisterSiteParams) && params instanceof HasOxdIdParams) {
            try {
                String oxdId = ((HasOxdIdParams) params).getOxdId();
                if (StringUtils.isNotBlank(oxdId)) {
                    final RpService rpService = ServerLauncher.getInjector().getInstance(RpService.class);
                    final Rp rp = rpService.getRp(oxdId);
                    if (rp != null) {
                        return new Pair<>(rp, false);
                    }
                }
            } catch (HttpException e) {
                // ignore
            } catch (Exception e) {
                LOG.error("Failed to identify RP. Message: " + e.getMessage(), e);
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
        if (params instanceof GetRpParams) {
            GetRpParams p = (GetRpParams) params;
            String oxdId = p.getOxdId();
            if (StringUtils.isNotBlank(oxdId) && (p.getList() == null || !p.getList())) {
                final RpService rpService = ServerLauncher.getInjector().getInstance(RpService.class);
                Rp rp = rpService.getRp(oxdId);
                if (rp != null) {
                    return new Pair<>(rp, true);
                }
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
        final OxdServerConfiguration configuration = ServerLauncher.getInjector().getInstance(ConfigurationService.class).get();
        if (configuration.getProtectCommandsWithAccessToken() != null && !configuration.getProtectCommandsWithAccessToken()) {
            return false; // skip validation since protectCommandsWithAccessToken=false
        }

        final String accessToken = params.getProtectionAccessToken();

        if (StringUtils.isBlank(accessToken)) {
            throw new HttpException(ErrorResponseCode.BLANK_PROTECTION_ACCESS_TOKEN);
        }
        if (params instanceof RegisterSiteParams) {
            return false; // skip validation for site registration because we have to associate oxd_id with client_id, validation is performed inside operation
        }

        final RpService rpService = ServerLauncher.getInjector().getInstance(RpService.class);

        final Rp rp = rpService.getRp(params.getOxdId());

        final IntrospectionResponse introspectionResponse = introspect(accessToken, params.getOxdId());

        LOG.trace("access_token: " + accessToken + ", introspection: " + introspectionResponse + ", clientId: " + rp.getClientId());
        if (StringUtils.isBlank(introspectionResponse.getClientId())) {
            throw new HttpException(ErrorResponseCode.NO_CLIENT_ID_IN_INTROSPECTION_RESPONSE);
        }
        if (!introspectionResponse.getScope().contains("oxd")) {
            throw new HttpException(ErrorResponseCode.PROTECTION_ACCESS_TOKEN_INSUFFICIENT_SCOPE);
        }

        if (introspectionResponse.getClientId().equals(rp.getClientId())) {
            return true;
        }
        throw new HttpException(ErrorResponseCode.INVALID_PROTECTION_ACCESS_TOKEN);
    }

    public IntrospectionResponse introspect(String accessToken, String oxdId) {
        if (StringUtils.isBlank(accessToken)) {
            throw new HttpException(ErrorResponseCode.BLANK_PROTECTION_ACCESS_TOKEN);
        }

        final RpService rpService = ServerLauncher.getInjector().getInstance(RpService.class);
        final Rp rp = rpService.getRp(oxdId);

        LOG.trace("Introspect token with rp: " + rp);

        final IntrospectionService introspectionService = ServerLauncher.getInjector().getInstance(IntrospectionService.class);
        final IntrospectionResponse response = introspectionService.introspectToken(oxdId, accessToken);

        if (!response.isActive()) {
            LOG.debug("access_token is not active.");
            throw new HttpException(ErrorResponseCode.INACTIVE_PROTECTION_ACCESS_TOKEN);
        }
        return response;
    }

    public void validate(HasOxdIdParams params) {
        notNull(params);
        notBlankOxdId(params.getOxdId());
    }

    public Rp validate(Rp rp) {
        if (rp == null) {
            throw new HttpException(ErrorResponseCode.INVALID_OXD_ID);
        }

        notBlankOxdId(rp.getOxdId());
        notBlankOpHost(rp.getOpHost());
        return rp;
    }
}
