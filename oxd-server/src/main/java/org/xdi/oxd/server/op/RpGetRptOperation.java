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
import org.xdi.oxd.common.params.RpGetRptParams;
import org.xdi.oxd.common.response.RpGetRptOpResponse;
import org.xdi.oxd.server.service.SiteConfiguration;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class RpGetRptOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RpGetRptOperation.class);

    protected RpGetRptOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {

        final RpGetRptParams params = asParams(RpGetRptParams.class);

        SiteConfiguration site = getSite(params.getOxdId());
        UmaConfiguration discovery = getDiscoveryService().getUmaDiscoveryByOxdId(params.getOxdId());

        final CreateRptService rptService = UmaClientFactory.instance().createRequesterPermissionTokenService(discovery, getHttpService().getClientExecutor());
        final RPTResponse rptResponse = rptService.createRPT("Bearer " + site.getAat(), site.getOpHost());
        if (rptResponse != null && StringUtils.isNotBlank(rptResponse.getRpt())) {
            final String rpt = rptResponse.getRpt();
            LOG.debug("RPT is successfully obtained from AS. RPT: {}", rpt);
            final RpGetRptOpResponse r = new RpGetRptOpResponse();
            r.setRpt(rpt);
            return okResponse(r);
        } else {
            LOG.error("Failed to get RPT for site: " + site);
        }
        return null;
    }
}
