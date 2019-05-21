package org.gluu.oxd.server.op;

import org.apache.commons.beanutils.BeanUtils;
import org.gluu.oxauth.client.OpenIdConfigurationResponse;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.response.GetOpDiscoveryConfigResponse;
import org.gluu.oxd.server.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.inject.Injector;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.params.GetOpDiscoveryConfigParams;
import org.gluu.oxd.common.response.IOpResponse;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetOpDiscoveryConfigOperation extends BaseOperation<GetOpDiscoveryConfigParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetOpDiscoveryConfigOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected GetOpDiscoveryConfigOperation(Command command, final Injector injector) {
        super(command, injector, GetOpDiscoveryConfigParams.class);
    }

    public IOpResponse execute(GetOpDiscoveryConfigParams params) throws IOException {
        OpenIdConfigurationResponse discoveryResponse = getDiscoveryService().getConnectDiscoveryResponse(params.getOpHost(), params.getOpDiscoveryPath());

        GetOpDiscoveryConfigResponse response = new GetOpDiscoveryConfigResponse();
        try {
            BeanUtils.copyProperties(response, discoveryResponse);
            return response;
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOG.error("Error in creating op discovery configuration response ", e.getMessage());
        }
        throw new HttpException(ErrorResponseCode.FAILED_TO_GET_OP_DISCOVERY);
    }
}
