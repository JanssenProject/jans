package io.jans.ca.server.op;

import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.GetRpParams;
import io.jans.ca.common.response.GetRpResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.configuration.model.MinimumRp;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.service.RpService;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@RequestScoped
@Named
public class GetRpOperation extends BaseOperation<GetRpParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetRpOperation.class);
    @Inject
    RpService rpService;

    @Override
    public IOpResponse execute(GetRpParams params, HttpServletRequest httpServletRequest) {
        if (params.getList() != null && params.getList()) {
            List<MinimumRp> rps = new ArrayList<>();
            for (Rp rp : rpService.getRps().values()) {
                rps.add(rp.asMinimumRp());
            }
            return new GetRpResponse(Jackson2.createJsonMapper().valueToTree(rps));
        }

        Rp rp = getRpSyncService().getRp(params.getRpId());
        if (rp != null) {
            return new GetRpResponse(Jackson2.createJsonMapper().valueToTree(rp));
        } else {
            LOG.trace("Failed to find RP by rp_id: " + params.getRpId());
        }
        return new GetRpResponse();
    }

    @Override
    public Class<GetRpParams> getParameterClass() {
        return GetRpParams.class;
    }

    @Override
    public String getReturnType() {
        return MediaType.APPLICATION_JSON;
    }

}
