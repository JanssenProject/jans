/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.uma.CreateRptService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.RPTResponse;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.ObtainRptParams;
import org.xdi.oxd.common.response.ObtainRptOpResponse;
import org.xdi.oxd.server.Utils;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class ObtainRptOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(ObtainRptOperation.class);

    protected ObtainRptOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {

            final ObtainRptParams params = asParams(ObtainRptParams.class);

                final String umaDiscoveryUrl = Utils.getUmaDiscoveryUrl(params.getAmHost());
                final UmaConfiguration umaDiscovery = getDiscoveryService().getUmaDiscovery(umaDiscoveryUrl);

                    final CreateRptService rptService = UmaClientFactory.instance().createRequesterPermissionTokenService(umaDiscovery, getHttpService().getClientExecutor());
                    final RPTResponse rptResponse = rptService.createRPT("Bearer " + params.getAat(), params.getAmHost());
                    if (rptResponse != null && StringUtils.isNotBlank(rptResponse.getRpt())) {
                        final String rpt = rptResponse.getRpt();
                        LOG.debug("RPT is successfully obtained from AS. RPT: {}", rpt);
                        final ObtainRptOpResponse r = new ObtainRptOpResponse();
                        r.setRptToken(rpt);
                        return okResponse(r);
                    }


        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }
}
