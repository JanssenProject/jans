package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import org.codehaus.jackson.JsonNode;
import org.gluu.oxauth.client.UserInfoClient;
import org.gluu.oxauth.client.UserInfoRequest;
import org.gluu.oxauth.client.UserInfoResponse;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.CoreUtils;
import org.gluu.oxd.common.params.GetUserInfoParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.POJOResponse;

import java.io.IOException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetUserInfoOperation extends BaseOperation<GetUserInfoParams> {

//    private static final Logger LOG = LoggerFactory.getLogger(GetUserInfoOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected GetUserInfoOperation(Command command, final Injector injector) {
        super(command, injector, GetUserInfoParams.class);
    }

    @Override
    public IOpResponse execute(GetUserInfoParams params) throws IOException {
        getValidationService().validate(params);

        UserInfoClient client = new UserInfoClient(getDiscoveryService().getConnectDiscoveryResponseByOxdId(params.getOxdId()).getUserInfoEndpoint());
        client.setExecutor(getHttpService().getClientExecutor());
        client.setRequest(new UserInfoRequest(params.getAccessToken()));

        final UserInfoResponse response = client.exec();
        return new POJOResponse(CoreUtils.createJsonMapper().readValue(response.getEntity(), JsonNode.class));
    }
}
