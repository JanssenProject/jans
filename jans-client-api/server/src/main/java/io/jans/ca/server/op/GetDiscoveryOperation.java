package io.jans.ca.server.op;

import io.jans.as.client.OpenIdConfigurationResponse;
import io.jans.ca.common.Command;
import io.jans.ca.common.CommandType;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.GetDiscoveryParams;
import io.jans.ca.common.params.IParams;
import io.jans.ca.common.response.GetDiscoveryResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.service.DiscoveryService;
import io.jans.ca.server.service.ServiceProvider;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import org.apache.commons.beanutils.BeanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;

@RequestScoped
@Named
public class GetDiscoveryOperation extends TemplateOperation<GetDiscoveryParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetDiscoveryOperation.class);

    @Inject
    DiscoveryService discoveryService;

    @Override
    public IOpResponse execute(GetDiscoveryParams params, HttpServletRequest httpRequest) {
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

    @Override
    public Class<GetDiscoveryParams> getParameterClass() {
        return GetDiscoveryParams.class;
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.GET_DISCOVERY;
    }

}
