package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.ErrorResponseCode;
import org.gluu.oxd.common.params.RemoveSiteParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.RemoveSiteResponse;
import org.gluu.oxd.server.HttpException;

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
