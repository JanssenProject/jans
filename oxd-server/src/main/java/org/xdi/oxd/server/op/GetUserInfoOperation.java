package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.xdi.oxauth.client.UserInfoClient;
import org.xdi.oxauth.client.UserInfoRequest;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.GetUserInfoParams;
import org.xdi.oxd.common.response.GetUserInfoResponse;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetUserInfoOperation extends BaseOperation {

//    private static final Logger LOG = LoggerFactory.getLogger(GetUserInfoOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected GetUserInfoOperation(Command command, final Injector injector) {
        super(command, injector);
    }

    @Override
    public CommandResponse execute() {
        final GetUserInfoParams params = asParams(GetUserInfoParams.class);
        getValidationService().validate(params);

        UserInfoClient client = new UserInfoClient(getDiscoveryService().getConnectDiscoveryResponseByOxdId(params.getOxdId()).getUserInfoEndpoint());
        client.setExecutor(getHttpService().getClientExecutor());
        client.setRequest(new UserInfoRequest(params.getAccessToken()));

        GetUserInfoResponse opResponse = new GetUserInfoResponse(client.exec().getClaims());
        return okResponse(opResponse);
    }
}
