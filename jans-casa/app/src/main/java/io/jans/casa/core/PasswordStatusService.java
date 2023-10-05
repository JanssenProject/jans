package io.jans.casa.core;

import io.jans.casa.conf.MainSettings;
import io.jans.casa.core.model.IdentityPerson;
import io.jans.casa.misc.Utils;
import org.slf4j.Logger;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.List;

@Named
@SessionScoped
public class PasswordStatusService implements Serializable {

    private static final String EXTERNAL_IDENTITIES_PREFIX = "passport-";

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
    private boolean password2faRequisite;

    public boolean isPassword2faRequisite() {
        return password2faRequisite;
    }

    public boolean isPassSetAvailable() {
        return passSetAvailable;
    }

    public boolean isPassResetAvailable() {
        return passResetAvailable;
    }

    public void reloadStatus() {
        /*
         offer pass set if
          - user has no password and
          - has oxExternalUid like passport-*
         offer pass reset if
          - user has password and
          - app config allows this
         offer 2fa when
          - user has password or
          - backend ldap detected
         */
        passResetAvailable = false;
        passSetAvailable = false;
        IdentityPerson p = persistenceService.get(IdentityPerson.class, persistenceService.getPersonDn(asco.getUser().getId()));

        if (p.hasPassword()) {
            passResetAvailable = confSettings.isEnablePassReset();
        } else {
            passSetAvailable = hasPassportPrefix(p.getJansExtUid()) || hasPassportPrefix(p.getJansUnlinkedExternalUids());
        }
        password2faRequisite = p.hasPassword() || persistenceService.isBackendLdapEnabled();

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

    private boolean hasPassportPrefix(List<String> externalUids) {
        return externalUids.stream().anyMatch(uid -> uid.startsWith(EXTERNAL_IDENTITIES_PREFIX));
    }

}
