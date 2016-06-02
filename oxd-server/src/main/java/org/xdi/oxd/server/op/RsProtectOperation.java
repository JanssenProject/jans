package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxauth.model.uma.UmaConfiguration;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.RsProtectParams;
import org.xdi.oxd.rs.protect.resteasy.PatProvider;
import org.xdi.oxd.rs.protect.resteasy.ResourceRegistrar;
import org.xdi.oxd.rs.protect.resteasy.ServiceProvider;
import org.xdi.oxd.server.service.SiteConfiguration;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

public class RsProtectOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RsProtectOperation.class);

    protected RsProtectOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() throws Exception {
        final RsProtectParams params = asParams(RsProtectParams.class);

        validate(params);

        SiteConfiguration site = getSite(params.getOxdId());
        UmaConfiguration umaDiscovery = getDiscoveryService().getUmaDiscoveryByOxdId(params.getOxdId());

        PatProvider patProvider = new PatProvider() {
            @Override
            public String getPatToken() {
                return getUmaTokenService().getPat(params.getOxdId()).getToken();
            }

            @Override
            public void clearPat() {
                // do nothing
            }
        };

        ResourceRegistrar resourceRegistrar = new ResourceRegistrar(patProvider, new ServiceProvider(site.getOpHost()));
        resourceRegistrar.register(params.getResources().getResources());

        return null;
    }

    private void validate(RsProtectParams params) {


    }


}
