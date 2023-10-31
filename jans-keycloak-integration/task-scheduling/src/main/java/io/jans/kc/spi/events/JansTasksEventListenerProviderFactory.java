package io.jans.kc.spi.events;


import org.keycloak.Config;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.utils.PostMigrationEvent;
import org.keycloak.timer.TimerProvider;
import org.keycloak.timer.TimerProviderFactory;

import io.jans.kc.spi.tasks.SamlConfigUpdateTask;

public class JansTasksEventListenerProviderFactory implements EventListenerProviderFactory {

    private static final String PROVIDER_ID = "kc-jans-event-listener";

    @Override
    public String getId() {

        return PROVIDER_ID;
    }

    @Override
    public EventListenerProvider create(KeycloakSession keycloakSession) {

        return new JansTasksEventListenerProvider();
    }

    @Override
    public void init(Config.Scope scope) {

    }

    @Override
    public void postInit(KeycloakSessionFactory keycloakSessionFactory) {

        keycloakSessionFactory.register(event -> {
            if( event instanceof PostMigrationEvent) {
                KeycloakSession kcsession = keycloakSessionFactory.create();
                TimerProviderFactory tpfactory = (TimerProviderFactory) keycloakSessionFactory.getProviderFactory(TimerProvider.class);
                SamlConfigUpdateTask task = new SamlConfigUpdateTask();
                tpfactory.create(kcsession).scheduleTask(task, task.getTaskInterval(), task.getTaskId());
            }
        });
    }

    @Override
    public void close() {

    }
}
