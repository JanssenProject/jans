package org.gluu.oxd;

import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.*;
import org.xdi.oxd.common.response.*;

import java.io.IOException;

public class Oxd {

    public static SetupClientResponse setupClient(SetupClientParams params) throws IOException {
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

    public static RegisterSiteResponse registerSite(RegisterSiteParams params, String p_authorization) throws IOException {
        CommandClient client = null;

        try {
            client = newClient();

            final Command command = new Command(CommandType.REGISTER_SITE);
            params.setProtectionAccessToken(p_authorization);
            command.setParamsObject(params);

            return client.send(command).dataAsResponse(RegisterSiteResponse.class);

        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static UpdateSiteResponse updateSite(UpdateSiteParams params, String p_authorization) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();

            final Command command = new Command(CommandType.UPDATE_SITE);
            params.setProtectionAccessToken(p_authorization);
            command.setParamsObject(params);

            return client.send(command).dataAsResponse(UpdateSiteResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static GetAuthorizationUrlResponse getAuthorizationUrl(GetAuthorizationUrlParams params, String p_authorization) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();

            final Command command = new Command(CommandType.GET_AUTHORIZATION_URL);
            params.setProtectionAccessToken(p_authorization);
            command.setParamsObject(params);

            return client.send(command).dataAsResponse(GetAuthorizationUrlResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static GetTokensByCodeResponse getTokenByCode(GetTokensByCodeParams params, String p_authorization) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();

            final Command command = new Command(CommandType.GET_TOKENS_BY_CODE);
            params.setProtectionAccessToken(p_authorization);
            command.setParamsObject(params);

            return client.send(command).dataAsResponse(GetTokensByCodeResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static GetUserInfoResponse getUserInfo(GetUserInfoParams params, String p_authorization) throws IOException {
        CommandClient client = null;

        try {
            client = newClient();

            final Command command = new Command(CommandType.GET_USER_INFO);
            params.setProtectionAccessToken(p_authorization);
            command.setParamsObject(params);

            return client.send(command).dataAsResponse(GetUserInfoResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static LogoutResponse getLogoutUri(GetLogoutUrlParams params, String p_authorization) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();

            final Command command = new Command(CommandType.GET_LOGOUT_URI);
            params.setProtectionAccessToken(p_authorization);
            command.setParamsObject(params);

            return client.send(command).dataAsResponse(LogoutResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static RsProtectResponse umaRsProtect(RsProtectParams params, String p_authorization) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();

            final Command command = new Command(CommandType.RS_PROTECT);
            params.setProtectionAccessToken(p_authorization);
            command.setParamsObject(params);

            return client.send(command).dataAsResponse(RsProtectResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static RsCheckAccessResponse umaRsCheckAccess(RsCheckAccessParams params, String p_authorization) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();

            final Command command = new Command(CommandType.RS_CHECK_ACCESS);
            params.setProtectionAccessToken(p_authorization);
            command.setParamsObject(params);

            return client.send(command).dataAsResponse(RsCheckAccessResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static RpGetRptResponse umaRpGetRpt(RpGetRptParams params, String p_authorization) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();

            final Command command = new Command(CommandType.RP_GET_RPT);
            params.setProtectionAccessToken(p_authorization);
            command.setParamsObject(params);

            return client.send(command).dataAsResponse(RpGetRptResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static RpGetClaimsGatheringUrlResponse umaRpGetClaimsGatheringUrl(RpGetClaimsGatheringUrlParams params, String p_authorization) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();

            final Command command = new Command(CommandType.RP_GET_CLAIMS_GATHERING_URL);
            params.setProtectionAccessToken(p_authorization);
            command.setParamsObject(params);

            return client.send(command).dataAsResponse(RpGetClaimsGatheringUrlResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static GetClientTokenResponse getAccessTokenByRefreshToken(GetAccessTokenByRefreshTokenParams params) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();

            final Command command = new Command(CommandType.GET_ACCESS_TOKEN_BY_REFRESH_TOKEN);
            command.setParamsObject(params);

            return client.send(command).dataAsResponse(GetClientTokenResponse.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public static GetClientTokenResponse getClientToken(GetClientTokenParams params) throws IOException {
        CommandClient client = null;
        try {
            client = newClient();
            return client.send(new Command(CommandType.GET_CLIENT_TOKEN).setParamsObject(params)).dataAsResponse(GetClientTokenResponse.class);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    private static CommandClient newClient() throws IOException {
        OxdHttpsConfiguration configuration = new OxdHttpsConfiguration();
        return new CommandClient(configuration.getDefaultHost(), Integer.parseInt(configuration.getDefaultPort()));
    }
}
