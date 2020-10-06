package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import org.apache.commons.beanutils.BeanUtils;
import org.gluu.oxauth.client.OpenIdConnectDiscoveryClient;
import org.gluu.oxauth.client.OpenIdConnectDiscoveryResponse;
import org.gluu.oxauth.model.discovery.WebFingerParam;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.params.GetIssuerParams;
import org.gluu.oxd.common.response.GetIssuerResponse;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.server.HttpException;
import org.python.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class GetIssuerOperation extends BaseOperation<GetIssuerParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetIssuerOperation.class);

    protected GetIssuerOperation(Command command, final Injector injector) {
        super(command, injector, GetIssuerParams.class);
    }

    public IOpResponse execute(GetIssuerParams params) {

        try {
            validateParams(params);
            OpenIdConnectDiscoveryClient client = new OpenIdConnectDiscoveryClient(params.getResource());
            OpenIdConnectDiscoveryResponse response = client.exec();
            if (response == null) {
                LOG.error("Error in fetching op discovery configuration response ");
                throw new HttpException(ErrorResponseCode.FAILED_TO_GET_ISSUER);
            }
            GetIssuerResponse webfingerResponse = new GetIssuerResponse();
            BeanUtils.copyProperties(webfingerResponse, response);

            String issuerFromDiscovery = getDiscoveryService().getConnectDiscoveryResponse(params.getOpConfigurationEndpoint(), params.getOpHost(), params.getOpDiscoveryPath()).getIssuer();
            validateIssuer(webfingerResponse, issuerFromDiscovery);

            return webfingerResponse;
        } catch (Exception e) {
            LOG.error("Error in creating op discovery configuration response ", e);
        }
        throw new HttpException(ErrorResponseCode.FAILED_TO_GET_ISSUER);
    }

    private static void validateParams(GetIssuerParams params) {
        if (Strings.isNullOrEmpty(params.getOpHost()) && Strings.isNullOrEmpty(params.getOpConfigurationEndpoint())) {
            LOG.error("Either 'op_configuration_endpoint' or 'op_host' should be provided.");
            throw new HttpException(ErrorResponseCode.INVALID_OP_HOST_AND_CONFIGURATION_ENDPOINT);
        }

        if (Strings.isNullOrEmpty(params.getResource())) {
            LOG.error("The 'resource' is empty or not specified.");
            throw new HttpException(ErrorResponseCode.BAD_REQUEST_NO_RESOURCE);
        }
    }

    private static void validateIssuer(GetIssuerResponse webfingerResponse, String issuerFromDiscovery) {

        List<String> locations = webfingerResponse.getLinks().stream().filter(webFingerLink -> webFingerLink.getRel().equals(WebFingerParam.REL_VALUE)).map(webFingerLink -> webFingerLink.getHref()).collect(Collectors.toList());
        if (locations.stream().noneMatch(webFingerLink -> webFingerLink.equals(issuerFromDiscovery))) {
            LOG.error("Discovered issuer not matched with issuer obtained from Webfinger. Got : {}, Expected : {}", issuerFromDiscovery, String.join(", ", locations));
            throw new HttpException(ErrorResponseCode.INVALID_ISSUER_DISCOVERED);
        }
    }
}
