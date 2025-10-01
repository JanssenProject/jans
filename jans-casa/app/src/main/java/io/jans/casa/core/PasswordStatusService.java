package io.jans.casa.core;

import io.jans.casa.conf.MainSettings;
import io.jans.casa.core.model.IdentityPerson;
import io.jans.casa.misc.Utils;
import io.jans.orm.search.filter.Filter;
import io.jans.model.JansAttribute;
import io.jans.model.attribute.AttributeValidation;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.io.Serializable;
import java.util.*;
import java.util.regex.Pattern;

import org.slf4j.Logger;

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

    public Boolean changePassword(String userId, String newPassword) {

        try {
            if (Utils.isNotEmpty(newPassword)) {

                if (confSettings.isUsePasswordPolicy() &&
                        !passwordValidationPassed(newPassword)) return null;
                
                IdentityPerson person = persistenceService.get(IdentityPerson.class, persistenceService.getPersonDn(userId));
                person.setPassword(newPassword);
                return persistenceService.modify(person);
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        return false;

    }

    private boolean passwordValidationPassed(String password) {
        
        try {

            List<JansAttribute> attrs = persistenceService.find(JansAttribute.class, "ou=attributes,o=jans",
                    Filter.createEqualityFilter("jansAttrName", "userPassword"), 0, 1, null);
            AttributeValidation av = attrs.get(0).getAttributeValidation();
            
            if (av == null) return true;
            
            int len = Optional.ofNullable(av.getMinLength()).orElse(0);
            if (len > 0 && password.length() < len) {
                logger.error("Password is required to have at least {} characters", len);
                return false;
            }
            
            len = Optional.ofNullable(av.getMaxLength()).orElse(0);
            if (len > 0 && password.length() > len) {
                logger.error("Password is required to have at most {} characters", len);
                return false;
            }
            
            String regex = av.getRegexp();
            if (regex != null && !Pattern.matches(regex, password)) {
                logger.error("Provided password does not match the regular expression {}", regex);
                return false;
            }
            
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            return false;
        }
        return true;
         
    }

}
