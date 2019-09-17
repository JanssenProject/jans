package org.gluu.oxd.server.op;

import com.google.inject.Injector;
import org.gluu.oxd.common.Command;
import org.gluu.oxd.common.params.UpdateSiteParams;
import org.gluu.oxd.common.response.IOpResponse;
import org.gluu.oxd.common.response.UpdateSiteResponse;
import org.gluu.oxd.server.service.Rp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 11/03/2016
 */

public class UpdateSiteOperation extends BaseOperation<UpdateSiteParams> {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateSiteOperation.class);

    /**
     * Base constructor
     *
     * @param command command
     */
    protected UpdateSiteOperation(Command command, final Injector injector) {
        super(command, injector, UpdateSiteParams.class);
    }

    @Override
    public IOpResponse execute(UpdateSiteParams params) {
        final Rp rp = getRp();

        LOG.info("Updating rp ... rp: " + rp);
        persistRp(rp, params);

        UpdateSiteResponse response = new UpdateSiteResponse();
        response.setOxdId(rp.getOxdId());
        return response;
    }

    private void persistRp(Rp rp, UpdateSiteParams params) {

        try {
            getUpdateRegisteredClientService().updateRegisteredClient(rp, params);
            getRpService().update(rp);

            LOG.info("RP updated: " + rp);
        } catch (Exception e) {
            throw new RuntimeException("Failed to persist RP, params: " + params, e);
        }
    }
}