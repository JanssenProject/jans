package io.jans.ca.server.op;

import com.google.common.collect.Lists;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.ExpiredObjectType;
import io.jans.ca.common.params.RpGetClaimsGatheringUrlParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.RpGetClaimsGatheringUrlResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.Utils;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.service.DiscoveryService;
import io.jans.ca.server.service.StateService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import org.apache.commons.lang.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequestScoped
@Named
public class RpGetGetClaimsGatheringUrlOperation extends BaseOperation<RpGetClaimsGatheringUrlParams> {

    @Inject
    DiscoveryService discoveryService;
    @Inject
    StateService stateService;

    @Override
    public IOpResponse execute(RpGetClaimsGatheringUrlParams params, HttpServletRequest httpServletRequest) throws Exception {
        validate(params);

        final UmaMetadata metadata = discoveryService.getUmaDiscoveryByRpId(params.getRpId());
        final Rp rp = getRp(params);
        final String state = StringUtils.isNotBlank(params.getState()) ? stateService.putState(stateService.encodeExpiredObject(params.getState(), ExpiredObjectType.STATE)) : stateService.generateState();

        String url = metadata.getClaimsInteractionEndpoint() +
                "?client_id=" + rp.getClientId() +
                "&ticket=" + params.getTicket() +
                "&claims_redirect_uri=" + params.getClaimsRedirectUri() +
                "&state=" + state;

        if (params.getCustomParameters() != null && !params.getCustomParameters().isEmpty()) {
            List<String> paramsList = Lists.newArrayList("rp_id", "client_id", "ticket", "state", "claims_redirect_uri");

            Map<String, String> customParameterMap = params.getCustomParameters().entrySet()
                    .stream()
                    .filter(map -> !paramsList.contains(map.getKey()))
                    .collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));

            if (!customParameterMap.isEmpty()) {
                url += "&" + Utils.mapAsStringWithEncodedValues(customParameterMap);
            }
        }

        final RpGetClaimsGatheringUrlResponse r = new RpGetClaimsGatheringUrlResponse();
        r.setUrl(url);
        r.setState(state);
        return r;
    }

    @Override
    public Class<RpGetClaimsGatheringUrlParams> getParameterClass() {
        return RpGetClaimsGatheringUrlParams.class;
    }

    @Override
    public boolean isAuthorizationRequired() {
        return true;
    }

    @Override
    public String getReturnType() {
        return MediaType.APPLICATION_JSON;
    }

    private void validate(RpGetClaimsGatheringUrlParams params) {
        if (StringUtils.isBlank(params.getTicket())) {
            throw new HttpException(ErrorResponseCode.NO_UMA_TICKET_PARAMETER);
        }
        if (StringUtils.isBlank(params.getClaimsRedirectUri())) {
            throw new HttpException(ErrorResponseCode.NO_UMA_CLAIMS_REDIRECT_URI_PARAMETER);
        }
    }
}