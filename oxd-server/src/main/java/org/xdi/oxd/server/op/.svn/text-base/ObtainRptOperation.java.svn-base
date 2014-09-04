package org.xdi.oxd.server.op;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.uma.RequesterPermissionTokenService;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.model.uma.MetadataConfiguration;
import org.xdi.oxauth.model.uma.RequesterPermissionTokenResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.ObtainRptParams;
import org.xdi.oxd.common.response.ObtainRptOpResponse;
import org.xdi.oxd.server.DiscoveryService;
import org.xdi.oxd.server.HttpService;
import org.xdi.oxd.server.Utils;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 02/01/2014
 */

public class ObtainRptOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(ObtainRptOperation.class);

    protected ObtainRptOperation(Command p_command) {
        super(p_command);
    }

    @Override
    public CommandResponse execute() {
        try {
            final ObtainRptParams params = asParams(ObtainRptParams.class);
            if (params != null) {
                final String umaDiscoveryUrl = Utils.getUmaDiscoveryUrl(params.getAmHost());
                final MetadataConfiguration umaDiscovery = DiscoveryService.getInstance().getUmaDiscovery(umaDiscoveryUrl);
                if (umaDiscovery != null) {
                    final RequesterPermissionTokenService rptService = UmaClientFactory.instance().createRequesterPermissionTokenService(umaDiscovery, HttpService.getInstance().getClientExecutor());
                    final RequesterPermissionTokenResponse rptResponse = rptService.getRequesterPermissionToken("Bearer " + params.getAat(), params.getAmHost());
                    if (rptResponse != null && StringUtils.isNotBlank(rptResponse.getToken())) {
                        final String rpt = rptResponse.getToken();
                        LOG.debug("RPT is successfully obtained from AS. RPT: {}", rpt);
                        final ObtainRptOpResponse r = new ObtainRptOpResponse();
                        r.setRptToken(rpt);
                        return okResponse(r);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }
}
