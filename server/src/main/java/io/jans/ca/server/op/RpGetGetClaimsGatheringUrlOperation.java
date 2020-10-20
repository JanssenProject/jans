package io.jans.ca.server.op;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.Utils;
import org.apache.commons.lang.StringUtils;
import io.jans.as.model.uma.UmaMetadata;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.ExpiredObjectType;
import io.jans.ca.common.params.RpGetClaimsGatheringUrlParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.RpGetClaimsGatheringUrlResponse;
import io.jans.ca.server.service.Rp;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 17/06/2016
 */

public class RpGetGetClaimsGatheringUrlOperation extends BaseOperation<RpGetClaimsGatheringUrlParams> {

//    private static final Logger LOG = LoggerFactory.getLogger(RpGetGetClaimsGatheringUrlOperation.class);

    protected RpGetGetClaimsGatheringUrlOperation(Command command, final Injector injector) {
        super(command, injector, RpGetClaimsGatheringUrlParams.class);
    }

    @Override
    public IOpResponse execute(RpGetClaimsGatheringUrlParams params) throws Exception {
        validate(params);

        final UmaMetadata metadata = getDiscoveryService().getUmaDiscoveryByRpId(params.getRpId());
        final Rp rp = getRp();
        final String state = StringUtils.isNotBlank(params.getState()) ? getStateService().putState(getStateService().encodeExpiredObject(params.getState(), ExpiredObjectType.STATE)) : getStateService().generateState();

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

    private void validate(RpGetClaimsGatheringUrlParams params) {
        if (StringUtils.isBlank(params.getTicket())) {
            throw new HttpException(ErrorResponseCode.NO_UMA_TICKET_PARAMETER);
        }
        if (StringUtils.isBlank(params.getClaimsRedirectUri())) {
            throw new HttpException(ErrorResponseCode.NO_UMA_CLAIMS_REDIRECT_URI_PARAMETER);
        }
    }
}