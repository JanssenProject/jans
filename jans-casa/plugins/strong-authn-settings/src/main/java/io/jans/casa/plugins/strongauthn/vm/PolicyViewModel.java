package io.jans.casa.plugins.strongauthn.vm;

import io.jans.casa.core.pojo.User;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.strongauthn.conf.Configuration;
import io.jans.casa.plugins.strongauthn.conf.EnforcementPolicy;
import io.jans.casa.plugins.strongauthn.model.TrustedDevice;
import io.jans.casa.plugins.strongauthn.service.StrongAuthSettingsService;
import io.jans.casa.service.ISessionContext;
import io.jans.casa.ui.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.util.Pair;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.jans.casa.plugins.strongauthn.conf.EnforcementPolicy.EVERY_LOGIN;

public class PolicyViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private boolean uiHasPreferredMethod;
    private boolean uiAllowedToSetPolicy;
    private Set<String> enforcementPolicies;
    private Set<String> enforcementPoliciesCopy;
    private List<TrustedDevice> trustedDevices;

    private StrongAuthSettingsService sass;
    private User user;
    private ISessionContext sessionContext;

    public boolean isUiHasPreferredMethod() {
        return uiHasPreferredMethod;
    }

    public boolean isUiAllowedToSetPolicy() {
        return uiAllowedToSetPolicy;
    }

    public Set<String> getEnforcementPolicies() {
        return enforcementPolicies;
    }

    public List<TrustedDevice> getTrustedDevices() {
        return trustedDevices;
    }

    @Init
    public void init() {

        logger.debug("Initializing ViewModel");

        sass = StrongAuthSettingsService.instance();
        Configuration settings = sass.getSettingsHandler().getSettings();
        user = Utils.managedBean(ISessionContext.class).getLoggedUser();

        uiHasPreferredMethod = user.getPreferredMethod() != null;
        uiAllowedToSetPolicy = settings.getEnforcement2FA().contains(EnforcementPolicy.CUSTOM);
        logger.trace("User has a preferred method: {}", uiHasPreferredMethod);
        logger.trace("Users are allowed to set their own policy: {}", uiAllowedToSetPolicy);

        Pair<Set<String>, List<TrustedDevice>> police = sass.get2FAPolicyData(user.getId());
        enforcementPolicies = police.getX();
        trustedDevices = police.getY();

        if (enforcementPolicies.isEmpty()) {
            resetToDefaultPolicy();
        }
        enforcementPoliciesCopy = new HashSet<>(enforcementPolicies);

    }

    @NotifyChange("enforcementPolicies")
    public void checkPolicy(boolean checked, String policy) {

        if (checked) {
            enforcementPolicies.add(policy);
        } else {
            enforcementPolicies.remove(policy);
        }
        if (enforcementPolicies.contains(EVERY_LOGIN.toString())) {
            resetToDefaultPolicy();
        }
        logger.debug("Enforcement policies are: {}", enforcementPolicies.toString());

    }

    public void updatePolicy() {

        logger.trace("Updating user's policies");
        if (sass.update2FAPolicies(user.getId(), enforcementPolicies)) {
            enforcementPoliciesCopy = new HashSet<>(enforcementPolicies);
            UIUtils.showMessageUI(true);
        } else {
            UIUtils.showMessageUI(false);
        }

    }

    @NotifyChange("trustedDevices")
    public void deleteDevice(int index) {
        logger.trace("Deleting user device");
        UIUtils.showMessageUI(sass.deleteTrustedDevice(user.getId(), trustedDevices, index));
    }

    @NotifyChange("enforcementPolicies")
    public void cancel() {
        enforcementPolicies = new HashSet<>(enforcementPoliciesCopy);
    }

    private void resetToDefaultPolicy() {
        enforcementPolicies = new HashSet<>(Collections.singleton(EVERY_LOGIN.toString()));
    }

}
