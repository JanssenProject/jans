package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.params.GetRpParams;
import org.xdi.oxd.common.response.GetRpResponse;
import org.xdi.oxd.common.response.IOpResponse;
import org.xdi.oxd.rs.protect.Jackson;
import org.xdi.oxd.server.service.Rp;

import java.util.HashSet;
import java.util.Set;

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
            Set<String> oxdIds = getRpService().getRps().keySet();
            return new GetRpResponse(Jackson.createJsonMapper().valueToTree(new HashSet<>(oxdIds)));
        }

        Rp rp = getRpService().getRp(params.getOxdId());
        if (rp != null) {
            return new GetRpResponse(Jackson.createJsonMapper().valueToTree(rp));
        } else {
            LOG.trace("Failed to find RP by oxd_id: " + params.getOxdId());
        }
        return new GetRpResponse();
    }
}
