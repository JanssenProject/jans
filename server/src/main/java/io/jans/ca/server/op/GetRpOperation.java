package io.jans.ca.server.op;

import com.google.inject.Injector;
import io.jans.ca.common.Command;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.GetRpParams;
import io.jans.ca.common.response.GetRpResponse;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.server.service.MinimumRp;
import io.jans.ca.server.service.Rp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * @author yuriyz
 */
public class GetRpOperation extends BaseOperation<GetRpParams> {

    private static final Logger LOG = LoggerFactory.getLogger(GetRpOperation.class);

    /**
     * Base constructor
     *
     * @param command  command
     * @param injector injector
     */
    protected GetRpOperation(Command command, Injector injector) {
        super(command, injector, GetRpParams.class);
    }

    @Override
    public IOpResponse execute(GetRpParams params) {
        if (params.getList() != null && params.getList()) {
            List<MinimumRp> rps = new ArrayList<>();
            for (Rp rp : getRpService().getRps().values()) {
                rps.add(rp.asMinimumRp());
            }
            return new GetRpResponse(Jackson2.createJsonMapper().valueToTree(rps));
        }

        Rp rp = getRpSyncService().getRp(params.getOxdId());
        if (rp != null) {
            return new GetRpResponse(Jackson2.createJsonMapper().valueToTree(rp));
        } else {
            LOG.trace("Failed to find RP by oxd_id: " + params.getOxdId());
        }
        return new GetRpResponse();
    }
}
