package io.jans.ca.server.service;

import com.google.common.base.Strings;
import io.jans.as.model.common.IntrospectionResponse;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.*;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.RpServerConfiguration;
import io.jans.ca.server.ServerLauncher;
import io.jans.util.Pair;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

/**
 * @author Yuriy Zabrovarnyy
 */

public class ValidationService {

    private static final Logger LOG = LoggerFactory.getLogger(ValidationService.class);
    final RpServerConfiguration configuration = ServerLauncher.getInjector().getInstance(ConfigurationService.class).get();

    private void notNull(IParams params) {
        if (params == null) {
            throw new HttpException(ErrorResponseCode.INTERNAL_ERROR_NO_PARAMS);
        }
    }

    public void notBlankRpId(String rpId) {
        if (Strings.isNullOrEmpty(rpId)) {
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_RP_ID);
        }
    }

    public void notBlankOpHost(String opHost) {
        if (Strings.isNullOrEmpty(opHost)) {
            throw new HttpException(ErrorResponseCode.INVALID_OP_HOST);
        }
    }

    public void validateOpConfigurationEndpoint(String opConfigurationEndpoint) {
        if (Strings.isNullOrEmpty(opConfigurationEndpoint) || !opConfigurationEndpoint.contains(DiscoveryService.WELL_KNOWN_CONNECT_PATH)) {
            throw new HttpException(ErrorResponseCode.INVALID_OP_CONFIGURATION_ENDPOINT);
        }
    }

    public void isOpHostAllowed(String opHost) {
        List<String> allowedOpHosts = configuration.getAllowedOpHosts();
        if (!Strings.isNullOrEmpty(opHost) && !allowedOpHosts.isEmpty()) {
            if (!allowedOpHosts.stream().anyMatch(allowedUrl -> {
                        try {
                            return (new URL(allowedUrl)).equals(new URL(opHost));
                        } catch (MalformedURLException e) {
                            throw new HttpException(ErrorResponseCode.INVALID_ALLOWED_OP_HOST_URL);
                        }
                    }
            )) {
                throw new HttpException(ErrorResponseCode.RESTRICTED_OP_HOST);
            }
        }
    }

    public Pair<Rp, Boolean> validate(IParams params) {
        notNull(params);
        if (isInstanceOfGetRpParamsWithList(params)) {
            return new Pair(null, true);
        }

        if (params instanceof HasRpIdParams) {
            validate((HasRpIdParams) params);
        }

        if (!(params instanceof RegisterSiteParams) && params instanceof HasRpIdParams) {
            try {
                String rpId = ((HasRpIdParams) params).getRpId();
                if (StringUtils.isNotBlank(rpId)) {
                    final RpSyncService rpSyncService = ServerLauncher.getInjector().getInstance(RpSyncService.class);
                    final Rp rp = rpSyncService.getRp(rpId);
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
            String rpId = p.getRpId();
            if (StringUtils.isNotBlank(rpId) && (p.getList() == null || !p.getList())) {
                final RpSyncService rpSyncService = ServerLauncher.getInjector().getInstance(RpSyncService.class);
                Rp rp = rpSyncService.getRp(rpId);
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
     * @param accessToken
     * @param rpId
     */
    public void validateAccessToken(String accessToken, String rpId) {

        if (StringUtils.isBlank(accessToken)) {
            throw new HttpException(ErrorResponseCode.BLANK_ACCESS_TOKEN);
        }

        final RpSyncService rpSyncService = ServerLauncher.getInjector().getInstance(RpSyncService.class);

        final Rp rp = rpSyncService.getRp(rpId);

        final IntrospectionResponse introspectionResponse = introspect(accessToken, rpId);

        LOG.trace("access_token: " + accessToken + ", introspection: " + introspectionResponse + ", clientId: " + rp.getClientId());
        if (StringUtils.isBlank(introspectionResponse.getClientId())) {
            LOG.error("AS returned introspection response with empty/blank client_id which is required by jans_client_api. Please check your AS installation and make sure AS return client_id for introspection call (CE 3.1.0 or later).");
            throw new HttpException(ErrorResponseCode.NO_CLIENT_ID_IN_INTROSPECTION_RESPONSE);
        }
        if (!introspectionResponse.getScope().contains("jans_client_api")) {
            LOG.error("access_token does not have `jans_client_api` scope. Make sure a) scope exists on AS b) register_site is registered with 'jans_client_api' scope c) get_client_token has 'jans_client_api' scope in request");
            throw new HttpException(ErrorResponseCode.ACCESS_TOKEN_INSUFFICIENT_SCOPE);
        }

        if (introspectionResponse.getClientId().equals(rp.getClientId())) {
            return;
        }
        LOG.error("No access token provided in Authorization header. Forbidden.");
        throw new HttpException(ErrorResponseCode.INVALID_ACCESS_TOKEN);
    }

    public IntrospectionResponse introspect(String accessToken, String rpId) {
        if (StringUtils.isBlank(accessToken)) {
            LOG.debug("access_token is blank. Command is protected by access_token, please provide valid token or otherwise switch off protection in configuration with protect_commands_with_access_token=false");
            throw new HttpException(ErrorResponseCode.BLANK_ACCESS_TOKEN);
        }

        final RpSyncService rpSyncService = ServerLauncher.getInjector().getInstance(RpSyncService.class);
        final Rp rp = rpSyncService.getRp(rpId);

        LOG.trace("Introspect token with rp: " + rp);

        final IntrospectionService introspectionService = ServerLauncher.getInjector().getInstance(IntrospectionService.class);
        final IntrospectionResponse response = introspectionService.introspectToken(rpId, accessToken);

        if (!response.isActive()) {
            LOG.error("access_token is not active.");
            throw new HttpException(ErrorResponseCode.INACTIVE_ACCESS_TOKEN);
        }
        return response;
    }

    public void validate(HasRpIdParams params) {
        notNull(params);
        notBlankRpId(params.getRpId());
    }

    public Rp validate(Rp rp) {
        if (rp == null) {
            throw new HttpException(ErrorResponseCode.INVALID_RP_ID);
        }

        notBlankRpId(rp.getRpId());
        notBlankOpHost(rp.getOpHost());
        isOpHostAllowed(rp.getOpHost());
        return rp;
    }

    private static boolean isInstanceOfGetRpParamsWithList(IParams params) {
        if (params instanceof GetRpParams) {
            GetRpParams p = (GetRpParams) params;
            return p.getList() != null && p.getList();
        }
        return false;
    }
}

