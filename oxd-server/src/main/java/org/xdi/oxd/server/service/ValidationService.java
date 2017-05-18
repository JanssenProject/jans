package org.xdi.oxd.server.service;

import com.google.common.base.Strings;
import org.apache.commons.lang.StringUtils;
import org.jboss.resteasy.client.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.service.IntrospectionService;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.HasOxdIdParams;
import org.xdi.oxd.common.params.HasProtectionAccessTokenParams;
import org.xdi.oxd.common.params.IParams;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.ServerLauncher;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/05/2016
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
        notNull(params);
        if (params instanceof HasOxdIdParams) {
            validate((HasOxdIdParams) params);
        }
        if (params instanceof HasProtectionAccessTokenParams) {
            validate((HasProtectionAccessTokenParams) params);
        }
    }

    private void validate(HasProtectionAccessTokenParams params) {
        final Configuration configuration = ServerLauncher.getInjector().getInstance(ConfigurationService.class).get();
        if (configuration.getProtectCommandsWithAccessToken() != null && !configuration.getProtectCommandsWithAccessToken()) {
            if (StringUtils.isBlank(params.getProtectionAccessToken())) {
                return; // skip validation since protectCommandsWithAccessToken=false
            } // otherwise if token is not blank then let it validate it
        }

        final String accessToken = params.getProtectionAccessToken();
        if (StringUtils.isBlank(accessToken)) {
            throw new ErrorResponseException(ErrorResponseCode.BLANK_PROTECTION_ACCESS_TOKEN);
        }

        final RpService rpService = ServerLauncher.getInjector().getInstance(RpService.class);
        final DiscoveryService discoveryService = ServerLauncher.getInjector().getInstance(DiscoveryService.class);
        final UmaTokenService umaTokenService = ServerLauncher.getInjector().getInstance(UmaTokenService.class);
        final HttpService httpService = ServerLauncher.getInjector().getInstance(HttpService.class);

        final Rp rp = rpService.getRp(params.getOxdId());
        if (StringUtils.isBlank(rp.getSetupOxdId())) {
            throw new ErrorResponseException(ErrorResponseCode.NO_SETUP_CLIENT_FOR_OXD_ID);
        }

        final Rp setupRp = rpService.getRp(rp.getSetupOxdId());
        LOG.trace("SetupRP: " + setupRp);

        if (setupRp == null || StringUtils.isBlank(setupRp.getClientId())) {
            throw new ErrorResponseException(ErrorResponseCode.NO_SETUP_CLIENT_FOR_OXD_ID);
        }

        final String introspectionEndpoint = discoveryService.getConnectDiscoveryResponseByOxdId(params.getOxdId()).getIntrospectionEndpoint();
        final IntrospectionService introspectionService = ProxyFactory.create(IntrospectionService.class, introspectionEndpoint, httpService.getClientExecutor());
        final IntrospectionResponse introspectionResponse = introspectionService.introspectToken("Bearer " + umaTokenService.getPat(params.getOxdId()).getToken(), accessToken);
        LOG.trace("access_token: " + accessToken + ", introspection: " + introspectionResponse + ", setupClientId: " + setupRp.getClientId());
        if (introspectionResponse.getClientId().equals(setupRp.getClientId())) {
            return;
        }

        throw new ErrorResponseException(ErrorResponseCode.INVALID_PROTECTION_ACCESS_TOKEN);
    }

    public void validate(HasOxdIdParams params) {
        notNull(params);
        notBlankOxdId(params.getOxdId());
    }

    public Rp validate(Rp site) {
        if (site == null) {
            throw new ErrorResponseException(ErrorResponseCode.INVALID_OXD_ID);
        }

        notBlankOxdId(site.getOxdId());
        notBlankOpHost(site.getOpHost());
        return site;
    }
}
