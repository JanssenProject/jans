package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.xdi.oxauth.client.UserInfoClient;
import org.xdi.oxauth.client.UserInfoRequest;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.params.GetUserInfoParams;
import org.xdi.oxd.common.response.GetUserInfoResponse;
import org.xdi.oxd.common.response.IOpResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public IOpResponse execute(GetUserInfoParams params) {
        getValidationService().validate(params);

        UserInfoClient client = new UserInfoClient(getDiscoveryService().getConnectDiscoveryResponseByOxdId(params.getOxdId()).getUserInfoEndpoint());
        client.setExecutor(getHttpService().getClientExecutor());
        client.setRequest(new UserInfoRequest(params.getAccessToken()));

        final Map<String, List<String>> claims = client.exec().getClaims();
        return new GetUserInfoResponse(normalize(claims));
    }

    private static Map<String, List<String>> normalize(Map<String, List<String>> claims) {
        Map<String, List<String>> result = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : claims.entrySet()) {
            if (entry.getValue() != null) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }
}
