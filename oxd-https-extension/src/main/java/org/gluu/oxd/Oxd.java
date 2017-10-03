package org.gluu.oxd;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.*;
import org.xdi.oxd.common.response.*;

import java.io.IOException;

public class Oxd {

    private static final Logger LOG = LoggerFactory.getLogger(Oxd.class);

    private final OxdHttpsConfiguration configuration;

    public Oxd(OxdHttpsConfiguration configuration) {
        this.configuration = configuration;
    }

    public SetupClientResponse setupClient(SetupClientParams params) throws IOException {
        CommandClient client = null;

        try {
            client = newClient();

            final Command command = new Command(CommandType.SETUP_CLIENT);
            command.setParamsObject(params);

            return client.send(command).dataAsResponse(SetupClientResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public RegisterSiteResponse registerSite(RegisterSiteParams params, String accessToken) throws IOException {
        CommandClient client = null;

        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.REGISTER_SITE).setParamsObject(params)).dataAsResponse(RegisterSiteResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public UpdateSiteResponse updateSite(UpdateSiteParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.UPDATE_SITE).setParamsObject(params)).dataAsResponse(UpdateSiteResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public GetAuthorizationUrlResponse getAuthorizationUrl(GetAuthorizationUrlParams params, String p_authorization) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(p_authorization);

            return client.send(new Command(CommandType.GET_AUTHORIZATION_URL).setParamsObject(params)).dataAsResponse(GetAuthorizationUrlResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public GetTokensByCodeResponse getTokenByCode(GetTokensByCodeParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.GET_TOKENS_BY_CODE).setParamsObject(params)).dataAsResponse(GetTokensByCodeResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public GetUserInfoResponse getUserInfo(GetUserInfoParams params, String accessToken) throws IOException {
        CommandClient client = null;

        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.GET_USER_INFO).setParamsObject(params)).dataAsResponse(GetUserInfoResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public LogoutResponse getLogoutUri(GetLogoutUrlParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.GET_LOGOUT_URI).setParamsObject(params)).dataAsResponse(LogoutResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public RsProtectResponse umaRsProtect(RsProtectParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.RS_PROTECT).setParamsObject(params)).dataAsResponse(RsProtectResponse.class);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public RsCheckAccessResponse umaRsCheckAccess(RsCheckAccessParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.RS_CHECK_ACCESS).setParamsObject(params)).dataAsResponse(RsCheckAccessResponse.class);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public RpGetRptResponse umaRpGetRpt(RpGetRptParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.RP_GET_RPT).setParamsObject(params)).dataAsResponse(RpGetRptResponse.class);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public RpGetClaimsGatheringUrlResponse umaRpGetClaimsGatheringUrl(RpGetClaimsGatheringUrlParams params, String accessToken) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            params.setProtectionAccessToken(accessToken);

            return client.send(new Command(CommandType.RP_GET_CLAIMS_GATHERING_URL).setParamsObject(params)).dataAsResponse(RpGetClaimsGatheringUrlResponse.class);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public GetClientTokenResponse getAccessTokenByRefreshToken(GetAccessTokenByRefreshTokenParams params) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            return client.send(new Command(CommandType.GET_ACCESS_TOKEN_BY_REFRESH_TOKEN).setParamsObject(params)).dataAsResponse(GetClientTokenResponse.class);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public GetClientTokenResponse getClientToken(GetClientTokenParams params) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            return client.send(new Command(CommandType.GET_CLIENT_TOKEN).setParamsObject(params)).dataAsResponse(GetClientTokenResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    private CommandClient newClient() throws IOException {
        return new CommandClient(configuration.getOxdHost(), Integer.parseInt(configuration.getOxdPort()));
    }
}
