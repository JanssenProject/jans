package org.xdi.oxd.rp.client;

import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.CheckIdTokenParams;
import org.xdi.oxd.common.params.GetAuthorizationUrlParams;
import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.common.response.CheckIdTokenResponse;
import org.xdi.oxd.common.response.GetAuthorizationUrlResponse;
import org.xdi.oxd.common.response.RegisterSiteResponse;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 15/10/2015
 */

public class RpSocketClient implements RpClient {

    private CommandClient client;

    private RegisterSiteResponse registrationDetails;

    protected RpSocketClient(String host, int port) {
        try {
            client = new CommandClient(host, port);
        } catch (IOException e) {
            throw new RuntimeException("Failed to open connection to " + host + ":" + port, e);
        }
    }

    public static RpSocketClient newSocketClient(String host, int port) {
        return new RpSocketClient(host, port);
    }

    @Override
    public RpClient register(String authorizationUrl) {
        final RegisterSiteParams params = new RegisterSiteParams();
        params.setAuthorizationRedirectUri(authorizationUrl);
        return register(params);
    }

    @Override
    public RpClient register(RegisterSiteParams params) {
        final Command command = new Command(CommandType.REGISTER_SITE);
        command.setParamsObject(params);

        registrationDetails = client.send(command).dataAsResponse(RegisterSiteResponse.class);
        return this;
    }

    @Override
    public RegisterSiteResponse getRegistrationDetails() {
        return registrationDetails;
    }

    @Override
    public String getOxdId() {
        return registrationDetails.getOxdId();
    }

    @Override
    public void close() {
        client.close();
    }

    @Override
    public String getAuthorizationUrl() {
        final GetAuthorizationUrlParams commandParams = new GetAuthorizationUrlParams();
        commandParams.setOxdId(getOxdId());

        final Command command = new Command(CommandType.GET_AUTHORIZATION_URL);
        command.setParamsObject(commandParams);

        final GetAuthorizationUrlResponse resp = client.send(command).dataAsResponse(GetAuthorizationUrlResponse.class);
        return resp.getAuthorizationUrl();
    }

    @Override
    public CheckIdTokenResponse validateIdToken(String idToken) {

        final CheckIdTokenParams params = new CheckIdTokenParams();
        params.setOxdId(getOxdId());
        params.setIdToken(idToken);

        final Command checkIdTokenCommand = new Command(CommandType.CHECK_ID_TOKEN);
        checkIdTokenCommand.setParamsObject(params);
        return client.send(checkIdTokenCommand).dataAsResponse(CheckIdTokenResponse.class);
    }
}
