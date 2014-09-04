package org.xdi.oxd.server.op;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.OpenIdConfigurationResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.common.params.DiscoveryParams;
import org.xdi.oxd.server.DiscoveryService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 09/08/2013
 */

public class DiscoveryOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(DiscoveryOperation.class);

    protected DiscoveryOperation(Command p_command) {
        super(p_command);
    }

    @Override
    public CommandResponse execute() {
        try {
            final DiscoveryParams params = asParams(DiscoveryParams.class);
            if (params != null) {
                final OpenIdConfigurationResponse response = DiscoveryService.getInstance().getDiscoveryResponse(params.getDiscoveryUrl());
                if (StringUtils.isNotBlank(response.getEntity())) {
                    final JsonNode node = CoreUtils.createJsonMapper().readTree(response.getEntity());
                    if (node != null) {
                        return CommandResponse.ok().setData(node);
                    } else {
                        LOG.error("Unable to parse discovery response.");
                    }
                } else {
                    LOG.error("Unable to parse discovery response.");
                }
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }
}
