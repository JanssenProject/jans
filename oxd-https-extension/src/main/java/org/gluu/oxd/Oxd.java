package org.gluu.oxd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.*;

import java.io.IOException;

public class Oxd {

    private static final Logger LOG = LoggerFactory.getLogger(Oxd.class);

    private final OxdHttpsConfiguration configuration;

    public Oxd(OxdHttpsConfiguration configuration) {
        this.configuration = configuration;
    }

    public CommandResponse setupClient(SetupClientParams params) throws IOException {
        CommandClient client = null;

        try {
            client = newClient();
            return client.send(new Command(CommandType.SETUP_CLIENT).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse registerSite(RegisterSiteParams params, String accessToken) throws IOException {
        CommandClient client = null;

        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.REGISTER_SITE).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse updateSite(UpdateSiteParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.UPDATE_SITE).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse removeSite(RemoveSiteParams params, String token) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(token);

            return client.send(new Command(CommandType.REMOVE_SITE).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse getAuthorizationUrl(GetAuthorizationUrlParams params, String p_authorization) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(p_authorization);

            return client.send(new Command(CommandType.GET_AUTHORIZATION_URL).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse getTokenByCode(GetTokensByCodeParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.GET_TOKENS_BY_CODE).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse getUserInfo(GetUserInfoParams params, String accessToken) throws IOException {
        CommandClient client = null;

        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.GET_USER_INFO).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse getLogoutUri(GetLogoutUrlParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.GET_LOGOUT_URI).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse umaRsProtect(RsProtectParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.RS_PROTECT).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse umaRsCheckAccess(RsCheckAccessParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.RS_CHECK_ACCESS).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse umaRpGetRpt(RpGetRptParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.RP_GET_RPT).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse umaRpGetClaimsGatheringUrl(RpGetClaimsGatheringUrlParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.RP_GET_CLAIMS_GATHERING_URL).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse getAccessTokenByRefreshToken(GetAccessTokenByRefreshTokenParams params) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            return client.send(new Command(CommandType.GET_ACCESS_TOKEN_BY_REFRESH_TOKEN).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse getClientToken(GetClientTokenParams params) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            return client.send(new Command(CommandType.GET_CLIENT_TOKEN).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse introspectAccessToken(IntrospectAccessTokenParams params) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            return client.send(new Command(CommandType.INTROSPECT_ACCESS_TOKEN).setParamsObject(params));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public CommandResponse introspectRpt(IntrospectRptParams params) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            return client.send(new Command(CommandType.INTROSPECT_RPT).setParamsObject(params));
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    private CommandClient newClient() throws IOException {
        return new CommandClient(configuration.getOxdHost(), Integer.parseInt(configuration.getOxdPort()));
    }
}
