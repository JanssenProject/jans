package io.jans.kc.spi.tasks;

import org.keycloak.models.KeycloakSession;
import org.keycloak.timer.ScheduledTask;

import org.jboss.logging.Logger;

public class SamlConfigUpdateTask implements ScheduledTask {

    private static final Logger logger = Logger.getLogger(SamlConfigUpdateTask.class);

    private static final String TASK_ID = "saml-config-update";
    private static final long TASK_INTERVAL = 1000 * 5; // Run task every five minutes 

    public String getTaskId() {

        return TASK_ID;
    }

    public long getTaskInterval() {

        return TASK_INTERVAL;
    }

    @Override
    public void run(KeycloakSession session) {
        // TODO Auto-generated method stub 
    }
    
}
