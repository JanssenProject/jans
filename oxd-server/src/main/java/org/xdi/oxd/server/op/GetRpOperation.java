package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.GetRpParams;
import org.xdi.oxd.common.response.GetRpResponse;
import org.xdi.oxd.rs.protect.Jackson;
import org.xdi.oxd.server.service.MinimumRp;
import org.xdi.oxd.server.service.Rp;

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
    public CommandResponse execute(GetRpParams params) {
        if (params.getList() != null && params.getList()) {
            List<MinimumRp> rps = new ArrayList<>();
            for (Rp rp : getRpService().getRps().values()) {
                rps.add(rp.asMinimumRp());
            }
            return okResponse(new GetRpResponse(Jackson.createJsonMapper().valueToTree(rps)));
        }

        Rp rp = getRpService().getRp(params.getOxdId());
        if (rp != null) {
            return okResponse(new GetRpResponse(Jackson.createJsonMapper().valueToTree(rp)));
        } else {
            LOG.trace("Failed to find RP by oxd_id: " + params.getOxdId());
        }
        return okResponse(new GetRpResponse());
    }
}
