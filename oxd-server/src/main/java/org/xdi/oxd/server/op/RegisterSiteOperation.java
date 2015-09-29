package org.xdi.oxd.server.op;

import com.google.inject.Injector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.server.service.SiteConfiguration;
import org.xdi.oxd.server.service.SiteConfigurationService;

import java.util.UUID;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 24/09/2015
 */

public class RegisterSiteOperation extends BaseOperation {

    private static final Logger LOG = LoggerFactory.getLogger(RegisterSiteOperation.class);

    /**
     * Base constructor
     *
     * @param p_command command
     */
    protected RegisterSiteOperation(Command p_command, final Injector injector) {
        super(p_command, injector);
    }

    @Override
    public CommandResponse execute() {
        try {
            String siteId = UUID.randomUUID().toString();

            persistSiteConfiguration(siteId);

            RegisterSiteResponse opResponse = new RegisterSiteResponse();
            opResponse.setSiteId(siteId);
            return okResponse(opResponse);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
        return CommandResponse.INTERNAL_ERROR_RESPONSE;
    }

    private void persistSiteConfiguration(String siteId) {
        final RegisterSiteParams params = asParams(RegisterSiteParams.class);
        final SiteConfigurationService siteService = getInjector().getInstance(SiteConfigurationService.class);
        final boolean persisted = siteService.persist(createSiteConfiguration(siteId, params));
        if (!persisted) {
            throw new RuntimeException("Failed to persist site configuration, params: " + params);
        }
    }

    private SiteConfiguration createSiteConfiguration(String siteId, RegisterSiteParams params) {
        final SiteConfiguration siteConf = new SiteConfiguration();
        siteConf.setOxdId(siteId);
        siteConf.setAcrValues(params.getAcrValues());
        siteConf.setApplicationType(params.getApplicationType());
        siteConf.setAuthorizationRedirectUri(params.getAuthorizationRedirectUri());
        siteConf.setClaimsLocales(params.getClaimsLocales());
        siteConf.setClientId(params.getClientId());
        siteConf.setClientSecret(params.getClientSecret());
        siteConf.setContacts(params.getContacts());
        siteConf.setGrantType(params.getGrantType());
        siteConf.setRedirectUris(params.getRedirectUris());
        siteConf.setResponseTypes(params.getResponseTypes());
        siteConf.setScope(params.getScope());
        siteConf.setUiLocales(params.getUiLocales());

        return siteConf;
    }
}