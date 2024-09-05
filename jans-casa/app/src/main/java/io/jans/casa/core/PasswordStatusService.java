package io.jans.casa.core;

import io.jans.casa.conf.MainSettings;
import io.jans.casa.core.model.IdentityPerson;
import io.jans.casa.misc.Utils;
import org.slf4j.Logger;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;

@Named
@SessionScoped
public class PasswordStatusService implements Serializable {

    @Inject
    private Logger logger;

    @Inject
    private PersistenceService persistenceService;

    @Inject
    private MainSettings confSettings;

    @Inject
    private SessionContext asco;

    private boolean passSetAvailable;
    private boolean passResetAvailable;

    public boolean isPassSetAvailable() {
        return passSetAvailable;
    }

    public boolean isPassResetAvailable() {
        return passResetAvailable;
    }

    public void reloadStatus() {

        IdentityPerson p = persistenceService.get(IdentityPerson.class, persistenceService.getPersonDn(asco.getUser().getId()));
        passSetAvailable = !p.hasPassword();
        passResetAvailable = p.hasPassword() && confSettings.isEnablePassReset();        

    }

    public boolean passwordMatch(String userName, String password) {

        boolean match = false;
        try {
            match = persistenceService.authenticate(userName, password);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return match;

    }

    public boolean changePassword(String userId, String newPassword) {

        boolean success = false;
        try {
            if (Utils.isNotEmpty(newPassword)) {
                IdentityPerson person = persistenceService.get(IdentityPerson.class, persistenceService.getPersonDn(userId));
                person.setPassword(newPassword);
                success = persistenceService.modify(person);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return success;

    }

}
