/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.server.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.xdi.oxd.server.Configuration;
import org.xdi.oxd.server.persistence.H2PersistenceProvider;
import org.xdi.oxd.server.persistence.PersistenceService;
import org.xdi.oxd.server.persistence.PersistenceServiceImpl;
import org.xdi.oxd.server.persistence.SqlPersistenceProvider;
import org.xdi.oxd.server.service.*;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/11/2014
 */

public class GuiceModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(ConfigurationService.class).in(Singleton.class);
        bind(SocketService.class).in(Singleton.class);
        bind(PublicOpKeyService.class).in(Singleton.class);
        bind(RpService.class).in(Singleton.class);
        bind(HttpService.class).in(Singleton.class);
        bind(IntrospectionService.class).in(Singleton.class);
        bind(SqlPersistenceProvider.class).to(H2PersistenceProvider.class).in(Singleton.class);
        bind(PersistenceService.class).to(PersistenceServiceImpl.class).in(Singleton.class);
        bind(MigrationService.class).in(Singleton.class);
        bind(DiscoveryService.class).in(Singleton.class);
        bind(ValidationService.class).in(Singleton.class);
        bind(StateService.class).in(Singleton.class);

        bind(Configuration.class).toProvider(ConfigurationService.class);
    }

}
