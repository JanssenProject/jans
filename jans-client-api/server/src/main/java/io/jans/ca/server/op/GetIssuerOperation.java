package io.jans.ca.server.op;

import io.jans.as.client.OpenIdConnectDiscoveryClient;
import io.jans.as.client.OpenIdConnectDiscoveryResponse;
import io.jans.as.model.discovery.WebFingerParam;
import io.jans.ca.common.CommandType;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.GetIssuerParams;
import io.jans.ca.common.response.GetIssuerResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.service.DiscoveryService;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.beanutils.BeanUtils;
import org.python.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class GetIssuerOperation extends BaseOperation<GetIssuerParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetIssuerOperation.class);

    @Inject
    DiscoveryService discoveryService;

    public IOpResponse execute(GetIssuerParams params, HttpServletRequest httpServletRequest) {
        validateParams(params);
        GetIssuerResponse webfingerResponse = getWebfingerResponse(params.getResource());

        String issuerFromDiscovery = discoveryService.getConnectDiscoveryResponse(params.getOpConfigurationEndpoint(), params.getOpHost(), params.getOpDiscoveryPath()).getIssuer();
        validateIssuer(webfingerResponse, issuerFromDiscovery);

        return webfingerResponse;
    }

    private static GetIssuerResponse getWebfingerResponse(String resource) {
        try {
            OpenIdConnectDiscoveryClient client = new OpenIdConnectDiscoveryClient(resource);
            OpenIdConnectDiscoveryResponse response = client.exec();
            if (response == null || Strings.isNullOrEmpty(response.getSubject()) || response.getLinks().isEmpty()) {
                LOG.error("Error in fetching op discovery configuration response ");
                throw new HttpException(ErrorResponseCode.FAILED_TO_GET_ISSUER);
            }

            GetIssuerResponse webfingerResponse = new GetIssuerResponse();
            BeanUtils.copyProperties(webfingerResponse, response);

            return webfingerResponse;

        } catch (Exception e) {
            LOG.error("Error in creating op discovery configuration response ", e);
            throw new HttpException(ErrorResponseCode.FAILED_TO_GET_ISSUER);
        }
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

    @Override
    public Class<GetIssuerParams> getParameterClass() {
        return GetIssuerParams.class;
    }

    @Override
    public CommandType getCommandType() {
        return CommandType.ISSUER_DISCOVERY;
    }
}
