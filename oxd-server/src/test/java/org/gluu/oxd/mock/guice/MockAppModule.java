package org.gluu.oxd.mock.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import org.gluu.oxd.mock.service.OpClientFactoryMockImpl;
import org.gluu.oxd.server.OxdServerConfiguration;
import org.gluu.oxd.server.op.OpClientFactory;
import org.gluu.oxd.server.persistence.H2PersistenceProvider;
import org.gluu.oxd.server.persistence.PersistenceService;
import org.gluu.oxd.server.persistence.PersistenceServiceImpl;
import org.gluu.oxd.server.persistence.SqlPersistenceProvider;
import org.gluu.oxd.server.service.*;

public class MockAppModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(OxdServerConfiguration.class).toProvider(ConfigurationService.class);

        bind(ConfigurationService.class).in(Singleton.class);
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
        bind(OpClientFactory.class).to(OpClientFactoryMockImpl.class).in(Singleton.class);
    }
}
