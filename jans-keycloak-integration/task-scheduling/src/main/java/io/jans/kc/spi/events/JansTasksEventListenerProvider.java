package io.jans.kc.spi.events;

import org.keycloak.events.Event;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.admin.AdminEvent;

public class JansTasksEventListenerProvider implements EventListenerProvider{

    @Override
    public void close() {
        // TODO Auto-generated method stub
    }

    @Override
    public void onEvent(Event event) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onEvent(AdminEvent event, boolean includeRepresentation) {
        // TODO Auto-generated method stub
    }
    
}
