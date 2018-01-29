package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.codehaus.jackson.node.POJONode;
import org.jboss.resteasy.client.ClientResponseFailure;
import org.jboss.resteasy.client.ProxyFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.client.service.IntrospectionService;
import org.xdi.oxauth.model.common.IntrospectionResponse;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.IntrospectAccessTokenParams;

/**
 * @author yuriyz
 */
public class IntrospectAccessTokenOperation extends BaseOperation<IntrospectAccessTokenParams> {

    private static final Logger LOG = LoggerFactory.getLogger(IntrospectAccessTokenOperation.class);

    /**
     * Base constructor
     *
     * @param command  command
     * @param injector injector
     */
    protected IntrospectAccessTokenOperation(Command command, Injector injector) {
        super(command, injector, IntrospectAccessTokenParams.class);
    }

    @Override
    public CommandResponse execute(IntrospectAccessTokenParams params) throws Exception {
        getValidationService().validate(params);

        final IntrospectionService introspectionService = ProxyFactory.create(IntrospectionService.class, getDiscoveryService().getConnectDiscoveryResponseByOxdId(params.getOxdId()).getIntrospectionEndpoint(), getHttpService().getClientExecutor());
        IntrospectionResponse response = null;

        try {
            response = introspectionService.introspectToken("Bearer " + getUmaTokenService().getPat(params.getOxdId()).getToken(), params.getAccessToken());
        } catch (ClientResponseFailure e) {
            int status = e.getResponse().getStatus();
            LOG.debug("Failed to introspect token. Entity: " + e.getResponse().getEntity(String.class) + ", status: " + status, e);
            if (status == 400 || status == 401) {
                LOG.debug("Try maybe PAT is lost on AS, force refresh PAT and re-try ...");
                getUmaTokenService().obtainPat(params.getOxdId()); // force to refresh PAT
                response = introspectionService.introspectToken("Bearer " + getUmaTokenService().getPat(params.getOxdId()).getToken(), params.getAccessToken());
            } else {
                throw e;
            }
        }

        return CommandResponse.ok().setData(new POJONode(response));
    }
}
