package io.jans.ca.server.op;

import com.google.inject.Injector;
import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.RemoveSiteParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.RemoveSiteResponse;
import io.jans.ca.server.HttpException;

/**
 * @author yuriyz
 */
public class RemoveSiteOperation extends BaseOperation<RemoveSiteParams> {

//    private static final Logger LOG = LoggerFactory.getLogger(RemoveSiteOperation.class);

    /**
     * Base constructor
     *
     * @param command  command
     * @param injector injector
     */
    protected RemoveSiteOperation(Command command, Injector injector) {
        super(command, injector, RemoveSiteParams.class);
    }

    @Override
    public IOpResponse execute(RemoveSiteParams params) {
        String oxdId = getRp().getOxdId();
        if (getRpService().remove(oxdId)) {
            return new RemoveSiteResponse(oxdId);
        }
        throw new HttpException(ErrorResponseCode.FAILED_TO_REMOVE_SITE);
    }
}
