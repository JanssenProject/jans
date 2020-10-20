package io.jans.ca.mock.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import io.jans.ca.server.service.*;
import io.jans.ca.mock.service.OpClientFactoryMockImpl;
import io.jans.ca.server.OxdServerConfiguration;
import io.jans.ca.server.op.OpClientFactory;
import io.jans.ca.server.persistence.service.PersistenceService;
import io.jans.ca.server.persistence.service.PersistenceServiceImpl;

public class MockAppModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(OxdServerConfiguration.class).toProvider(ConfigurationService.class);

        bind(ConfigurationService.class).in(Singleton.class);
        bind(PublicOpKeyService.class).in(Singleton.class);
        bind(RpService.class).in(Singleton.class);
        bind(HttpService.class).in(Singleton.class);
        bind(IntrospectionService.class).in(Singleton.class);
        bind(PersistenceService.class).to(PersistenceServiceImpl.class).in(Singleton.class);
        bind(MigrationService.class).in(Singleton.class);
        bind(DiscoveryService.class).in(Singleton.class);
        bind(ValidationService.class).in(Singleton.class);
        bind(StateService.class).in(Singleton.class);
        bind(OpClientFactory.class).to(OpClientFactoryMockImpl.class).in(Singleton.class);
    }
}
