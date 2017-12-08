package org.xdi.oxd.server.op;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.ErrorResponseCode;
import org.xdi.oxd.common.ErrorResponseException;
import org.xdi.oxd.common.params.RsProtectParams;
import org.xdi.oxd.common.response.RsProtectResponse;
import org.xdi.oxd.rs.protect.RsResource;
import org.xdi.oxd.rs.protect.resteasy.Key;
import org.xdi.oxd.rs.protect.resteasy.PatProvider;
import org.xdi.oxd.rs.protect.resteasy.ResourceRegistrar;
import org.xdi.oxd.rs.protect.resteasy.ServiceProvider;
import org.xdi.oxd.server.model.UmaResource;
import org.xdi.oxd.server.service.Rp;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 31/05/2016
 */

public class RsProtectOperation extends BaseOperation<RsProtectParams> {

    private static final Logger LOG = LoggerFactory.getLogger(RsProtectOperation.class);

    protected RsProtectOperation(Command p_command, final Injector injector) {
        super(p_command, injector, RsProtectParams.class);
    }

    @Override
    public CommandResponse execute(final RsProtectParams params) throws Exception {
        validate(params);

        Rp site = getRp();

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

        ResourceRegistrar registrar = new ResourceRegistrar(patProvider, new ServiceProvider(site.getOpHost()));
        registrar.register(params.getResources());

        persist(registrar, site);

        return okResponse(new RsProtectResponse(site.getOxdId()));
    }

    private void persist(ResourceRegistrar registrar, Rp site) throws IOException {
        Map<Key, RsResource> resourceMapCopy = registrar.getResourceMapCopy();

        for (Map.Entry<Key, String> entry : registrar.getIdMapCopy().entrySet()) {
            UmaResource resource = new UmaResource();
            resource.setId(entry.getValue());
            resource.setPath(entry.getKey().getPath());
            resource.setHttpMethods(entry.getKey().getHttpMethods());

            Set<String> scopes = Sets.newHashSet();
            Set<String> scopesForTicket = Sets.newHashSet();
            for (String httpMethod : entry.getKey().getHttpMethods()) {
                scopes.addAll(resourceMapCopy.get(entry.getKey()).scopes(httpMethod));
                scopesForTicket.addAll(resourceMapCopy.get(entry.getKey()).getScopesForTicket(httpMethod));
            }

            resource.setScopes(Lists.newArrayList(scopes));
            resource.setTicketScopes(Lists.newArrayList(scopesForTicket));

            site.getUmaProtectedResources().add(resource);
        }

        getRpService().update(site);
    }

    private void validate(RsProtectParams params) {
        if (params.getResources() == null || params.getResources().isEmpty()) {
            throw new ErrorResponseException(ErrorResponseCode.NO_UMA_RESOURCES_TO_PROTECT);
        }
    }
}
