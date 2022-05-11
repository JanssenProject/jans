package io.jans.ca.server.op;

import io.jans.ca.common.Command;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.GetRpParams;
import io.jans.ca.common.response.GetRpResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.configuration.model.MinimumRp;
import io.jans.ca.server.configuration.model.Rp;
import io.jans.ca.server.service.RpService;
import io.jans.ca.server.service.RpSyncService;
import io.jans.ca.server.service.ServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yuriyz
 */
public class GetRpOperation extends BaseOperation<GetRpParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetRpOperation.class);
    private RpService rpService;
    private RpSyncService rpSyncService;

    public GetRpOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, GetRpParams.class);
        this.rpService = serviceProvider.getRpService();
        this.rpSyncService = serviceProvider.getRpSyncService();
    }

    @Override
    public IOpResponse execute(GetRpParams params) {
        if (params.getList() != null && params.getList()) {
            List<MinimumRp> rps = new ArrayList<>();
            for (Rp rp : rpService.getRps().values()) {
                rps.add(rp.asMinimumRp());
            }
            return new GetRpResponse(Jackson2.createJsonMapper().valueToTree(rps));
        }

        Rp rp = rpSyncService.getRp(params.getRpId());
        if (rp != null) {
            return new GetRpResponse(Jackson2.createJsonMapper().valueToTree(rp));
        } else {
            LOG.trace("Failed to find RP by rp_id: " + params.getRpId());
        }
        return new GetRpResponse();
    }
}
