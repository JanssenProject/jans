package io.jans.ca.server.op;

import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.GetDiscoveryParams;
import io.jans.ca.common.response.GetDiscoveryResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.service.DiscoveryService;
import io.jans.ca.server.service.ServiceProvider;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/09/2015
 */

public class GetDiscoveryOperation extends BaseOperation<GetDiscoveryParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetDiscoveryOperation.class);

    private DiscoveryService discoveryService;

    /**
     * Base constructor
     *
     * @param command command
     */
    public GetDiscoveryOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, GetDiscoveryParams.class);
        this.discoveryService = serviceProvider.getDiscoveryService();
    }

    public IOpResponse execute(GetDiscoveryParams params) {
        OpenIdConfigurationResponse discoveryResponse = discoveryService.getConnectDiscoveryResponse(params.getOpConfigurationEndpoint(), params.getOpHost(), params.getOpDiscoveryPath());

        GetDiscoveryResponse response = new GetDiscoveryResponse();
        try {
            BeanUtils.copyProperties(response, discoveryResponse);
            return response;
        } catch (IllegalAccessException | InvocationTargetException e) {
            LOG.error("Error in creating op discovery configuration response ", e);
        }
        throw new HttpException(ErrorResponseCode.FAILED_TO_GET_DISCOVERY);
    }
}
