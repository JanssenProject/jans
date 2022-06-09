package io.jans.ca.server.op;

import io.jans.ca.common.Command;
import io.jans.ca.common.ErrorResponseCode;
import io.jans.ca.common.params.RemoveSiteParams;
import io.jans.ca.common.response.IOpResponse;
import io.jans.ca.common.response.RemoveSiteResponse;
import io.jans.ca.server.HttpException;
import io.jans.ca.server.service.RpService;
import io.jans.ca.server.service.ServiceProvider;

/**
 * @author yuriyz
 */
public class RemoveSiteOperation extends BaseOperation<RemoveSiteParams> {

    private RpService rpService;

    public RemoveSiteOperation(Command command, ServiceProvider serviceProvider) {
        super(command, serviceProvider, RemoveSiteParams.class);
        this.rpService = serviceProvider.getRpService();

    }

    @Override
    public IOpResponse execute(RemoveSiteParams params) {
        String rpId = getRp().getRpId();
        if (rpService.remove(rpId)) {
            return new RemoveSiteResponse(rpId);
        }
        throw new HttpException(ErrorResponseCode.FAILED_TO_REMOVE_SITE);
    }
}
