package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.GetRpParams;
import org.xdi.oxd.common.response.GetRpResponse;
import org.xdi.oxd.rs.protect.Jackson;

import java.io.IOException;

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
        String rp = null;
        try {
            rp = Jackson.asJson(getRpService().getRp(params.getOxdId()));
        } catch (IOException e) {
            LOG.error("Failed to fetch RP by oxd_id: " + params.getOxdId());
        }
        return okResponse(new GetRpResponse(rp));
    }
}
