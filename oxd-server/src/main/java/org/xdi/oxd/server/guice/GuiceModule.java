/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.license.LicenseService;
import org.xdi.oxd.server.service.ConfigurationService;
import org.xdi.oxd.server.service.DiscoveryService;
import org.xdi.oxd.server.service.HttpService;
import org.xdi.oxd.server.service.SiteConfigurationService;
import org.xdi.oxd.server.service.SocketService;
import org.xdi.oxd.server.service.ValidationService;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/11/2014
 */

public class GuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(LicenseService.class).in(Singleton.class);
        bind(ConfigurationService.class).in(Singleton.class);
        bind(SocketService.class).in(Singleton.class);
        bind(SiteConfigurationService.class).in(Singleton.class);
        bind(HttpService.class).in(Singleton.class);
        bind(DiscoveryService.class).in(Singleton.class);
        bind(ValidationService.class).in(Singleton.class);

        bind(Configuration.class).toProvider(ConfigurationService.class);
    }

}
