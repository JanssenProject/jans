package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.codehaus.jackson.node.POJONode;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.uma.UmaClientFactory;
import org.xdi.oxauth.client.uma.UmaRptIntrospectionService;
import org.xdi.oxauth.model.uma.RptIntrospectionResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.IntrospectRptParams;

/**
 * @author yuriyz
 */
public class IntrospectRptOperation extends BaseOperation<IntrospectRptParams> {

    private static final Logger LOG = LoggerFactory.getLogger(IntrospectRptOperation.class);

    /**
     * Base constructor
     *
     * @param command  command
     * @param injector injector
     */
    protected IntrospectRptOperation(Command command, Injector injector) {
        super(command, injector, IntrospectRptParams.class);
    }

    @Override
    public CommandResponse execute(IntrospectRptParams params) throws Exception {
        getValidationService().validate(params);

        UmaRptIntrospectionService rptStatusService = UmaClientFactory.instance().createRptStatusService(getDiscoveryService().getUmaDiscoveryByOxdId(params.getOxdId()), getHttpService().getClientExecutor());
        RptIntrospectionResponse response;

        try {
            response = rptStatusService.requestRptStatus("Bearer " + getUmaTokenService().getPat(params.getOxdId()).getToken(), params.getRpt(), "");
        } catch (ClientResponseFailure e) {
            int status = e.getResponse().getStatus();
            LOG.debug("Failed to introspect rpt. Entity: " + e.getResponse().getEntity(String.class) + ", status: " + status, e);
            if (status == 400 || status == 401) {
                LOG.debug("Try maybe PAT is lost on AS, force refresh PAT and re-try ...");
                getUmaTokenService().obtainPat(params.getOxdId()); // force to refresh PAT
                response = rptStatusService.requestRptStatus("Bearer " + getUmaTokenService().getPat(params.getOxdId()).getToken(), params.getRpt(), "");
            } else {
                throw e;
            }
        }

        return CommandResponse.ok().setData(new POJONode(response));
    }
}
