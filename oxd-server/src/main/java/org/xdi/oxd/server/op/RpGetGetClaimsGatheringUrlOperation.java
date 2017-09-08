package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.xdi.oxauth.model.uma.UmaMetadata;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.RpGetClaimsGatheringUrlParams;
import org.xdi.oxd.common.response.RpGetClaimsGatheringUrlResponse;
import org.xdi.oxd.server.service.Rp;

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
    public CommandResponse execute(RpGetClaimsGatheringUrlParams params) {
        validate(params);

        final UmaMetadata metadata = getDiscoveryService().getUmaDiscoveryByOxdId(params.getOxdId());
        final Rp rp = getRp();
        final String state = getStateService().generateState();

        String url = metadata.getClaimsInteractionEndpoint() +
                "?client_id=" + rp.getClientId() +
                "&ticket=" + params.getTicket() +
                "&claims_redirect_uri=" + params.getClaimsRedirectUri() +
                "&state=" + state;

        final RpGetClaimsGatheringUrlResponse r = new RpGetClaimsGatheringUrlResponse();
        r.setUrl(url);
        r.setState(state);
        return okResponse(r);
    }

    private void validate(RpGetClaimsGatheringUrlParams params) {
        if (StringUtils.isBlank(params.getTicket())) {
            throw new ErrorResponseException(ErrorResponseCode.NO_UMA_TICKET_PARAMETER);
        }
        if (StringUtils.isBlank(params.getClaimsRedirectUri())) {
            throw new ErrorResponseException(ErrorResponseCode.NO_UMA_CLAIMS_REDIRECT_URI_PARAMETER);
        }
    }
}