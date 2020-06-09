package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import org.apache.commons.beanutils.BeanUtils;
import org.gluu.oxauth.client.OpenIdConnectDiscoveryClient;
import org.gluu.oxauth.client.OpenIdConnectDiscoveryResponse;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.params.GetIssuerParams;
import org.gluu.oxd.common.response.GetIssuerResponse;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.server.HttpException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;

public class GetIssuerOperation extends BaseOperation<GetIssuerParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetIssuerOperation.class);

    protected GetIssuerOperation(Command command, final Injector injector) {
        super(command, injector, GetIssuerParams.class);
    }

    public IOpResponse execute(GetIssuerParams params) {

        try {
            OpenIdConnectDiscoveryClient client = new OpenIdConnectDiscoveryClient(params.getResource());
            OpenIdConnectDiscoveryResponse response = client.exec();
            if (response == null) {
                LOG.error("Error in fetching op discovery configuration response ");
                throw new HttpException(ErrorResponseCode.FAILED_TO_GET_ISSUER);
            }
            GetIssuerResponse webfingerResponse = new GetIssuerResponse();
            BeanUtils.copyProperties(webfingerResponse, response);

            return webfingerResponse;
        } catch (Exception e) {
            LOG.error("Error in creating op discovery configuration response ", e);
        }
        throw new HttpException(ErrorResponseCode.FAILED_TO_GET_ISSUER);
    }
}
