package org.xdi.oxd.server.service;

import com.google.common.base.Strings;
import com.google.inject.Inject;
import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.service.ClientFactory;
import org.xdi.oxauth.client.service.IntrospectionService;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.HasOxdIdParams;
import org.xdi.oxd.common.params.HasProtectionAccessTokenParams;
import org.xdi.oxd.common.params.IParams;
import org.xdi.oxd.server.Configuration;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 23/05/2016
 */

public class ValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationService.class);

    private final Configuration configuration;
    private final Injector injector;

    @Inject
    public ValidationService(Injector injector, Configuration configuration) {
        this.configuration = configuration;
        this.injector = injector;
    }

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
        if (configuration.getProtectCommandsWithAccessToken() != null && !configuration.getProtectCommandsWithAccessToken()) {
            return; // skip validation since protectCommandsWithAccessToken=false
        }

        final String accessToken = params.getProtectionAccessToken();
        if (StringUtils.isBlank(accessToken)) {
            throw new ErrorResponseException(ErrorResponseCode.BLANK_PROTECTION_ACCESS_TOKEN);
        }

        final RpService rpService = injector.getInstance(RpService.class);
        final DiscoveryService discoveryService = injector.getInstance(DiscoveryService.class);
        final UmaTokenService umaTokenService = injector.getInstance(UmaTokenService.class);

        final Rp rp = rpService.getRp(params.getOxdId());
        if (StringUtils.isBlank(rp.getSetupOxdId())) {
            throw new ErrorResponseException(ErrorResponseCode.NO_SETUP_CLIENT_FOR_OXD_ID);
        }

        final Rp setupRp = rpService.getRp(rp.getSetupOxdId());
        LOG.trace("SetupRP: " + setupRp);

        if (setupRp == null || StringUtils.isBlank(setupRp.getClientId())) {
            throw new ErrorResponseException(ErrorResponseCode.NO_SETUP_CLIENT_FOR_OXD_ID);
        }

        final IntrospectionService introspectionService = ClientFactory.instance().createIntrospectionService(discoveryService.getConnectDiscoveryResponseByOxdId(params.getOxdId()).getIntrospectionEndpoint());
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
