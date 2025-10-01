package io.jans.casa.plugins.strongauthn.vm;

import io.jans.casa.conf.Basic2FASettings;
import io.jans.casa.core.pojo.User;
import io.jans.casa.misc.Utils;
import io.jans.casa.plugins.strongauthn.conf.TrustedDevicesSettings;
import io.jans.casa.plugins.strongauthn.conf.Configuration;
import io.jans.casa.plugins.strongauthn.conf.EnforcementPolicy;
import io.jans.casa.plugins.strongauthn.service.StrongAuthSettingsService;
import io.jans.casa.service.ISessionContext;
import io.jans.casa.service.settings.IPluginSettingsHandler;
import io.jans.casa.ui.UIUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.BindUtils;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zul.Messagebox;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.jans.casa.plugins.strongauthn.StrongAuthnSettingsPlugin.*;
import static io.jans.casa.plugins.strongauthn.conf.EnforcementPolicy.*;

/**
 * @author jgomer
 */
public class StrongAuthViewModel {

    private static final Pair<Integer, Integer> BOUNDS_MINCREDS_2FA = new Pair<>(1, 3);
    private Logger logger = LoggerFactory.getLogger(getClass());

    private IPluginSettingsHandler<Configuration> settingsHandler;
    private Configuration settings;
    private User user;
    
    private int minCreds2FA;
    private List<Integer> minCredsList;
    private Set<String> enforcementPolicies;
    private int locationExpiration;
    private int deviceExpiration;
    private boolean autoEnable;
    private boolean hideSwitch;
    private boolean selectPreferred;

    public int getLocationExpiration() {
        return locationExpiration;
    }

    public int getDeviceExpiration() {
        return deviceExpiration;
    }

    public List<Integer> getMinCredsList() {
        return minCredsList;
    }

    public int getMinCreds2FA() {
        return minCreds2FA;
    }

    public Set<String> getEnforcementPolicies() {
        return enforcementPolicies;
    }
    
    public boolean isAutoEnable() {
    	return autoEnable;
    }
    
    public boolean isHideSwitch() {
    	return hideSwitch;
    }
    
    public boolean isSelectPreferred() {
    	return selectPreferred;
    }

    public void setLocationExpiration(int locationExpiration) {
        this.locationExpiration = locationExpiration;
    }

    public void setDeviceExpiration(int deviceExpiration) {
        this.deviceExpiration = deviceExpiration;
    }

    public void setHideSwitch(boolean hideSwitch) {
        this.hideSwitch = hideSwitch;
    }

    public void setSelectPreferred(boolean selectPreferred) {
        this.selectPreferred = selectPreferred;
    }

    @Init
    public void init() {
        logger.debug("Initializing ViewModel");
        settingsHandler = StrongAuthSettingsService.instance().getSettingsHandler();
        settings = settingsHandler.getSettings();
        user = Utils.managedBean(ISessionContext.class).getLoggedUser();
        reloadConfig();
    }

    @NotifyChange({"autoEnable", "hideSwitch"})
    public void checkAutoEnable(boolean checked) {
    	autoEnable = checked;
    	if (!checked) {
    		hideSwitch = false;
    	}
    }
    
    @NotifyChange({"enforcementPolicies", "deviceExpiration", "locationExpiration"})
    public void checkPolicy(boolean checked, String policy) {

        logger.trace("Policy '{}' {}", policy, checked ? "checked" : "unchecked");
        if (checked) {
            enforcementPolicies.add(policy);
        } else {
            enforcementPolicies.remove(policy);
            //Revert the numbers, this helps prevent entering negative numbers, then deselecting an option, and then saving
            if (settings.getTrustedDevicesSettings() != null && (LOCATION_UNKNOWN.toString().equals(policy) || CUSTOM.toString().equals(policy))) {
                deviceExpiration = settings.getTrustedDevicesSettings().getDeviceExpirationDays();
            }
            if (settings.getTrustedDevicesSettings() != null && (DEVICE_UNKNOWN.toString().equals(policy) || CUSTOM.toString().equals(policy))) {
                locationExpiration = settings.getTrustedDevicesSettings().getLocationExpirationDays();
            }
        }
        if (enforcementPolicies.contains(EVERY_LOGIN.toString())) {
            enforcementPolicies = Stream.of(EVERY_LOGIN.toString()).collect(Collectors.toSet());
        } else if (enforcementPolicies.contains(CUSTOM.toString())) {
            enforcementPolicies = Stream.of(CUSTOM.toString()).collect(Collectors.toSet());
        }
        logger.trace("Newer enforcement policies: {}", enforcementPolicies.toString());

    }

    public void change2FASettings(Integer val) {

        val += BOUNDS_MINCREDS_2FA.getX();

        if (val == 1) {     //only one sucks
            promptBefore2FAProceed(Labels.getLabel("adm.strongauth_warning_one"), val);
        } else if (val > minCreds2FA) {   //maybe problematic...
            promptBefore2FAProceed(Labels.getLabel("adm.strongauth_warning_up", new Integer[]{ minCreds2FA }), val);
        } else {
            processUpdate(val);
        }

    }

    private void reloadConfig() {

        Optional<TrustedDevicesSettings> opt = Optional.ofNullable(settings.getTrustedDevicesSettings());
        locationExpiration = opt.map(TrustedDevicesSettings::getLocationExpirationDays).orElse(TRUSTED_LOCATION_EXPIRATION_DAYS);
        deviceExpiration = opt.map(TrustedDevicesSettings::getDeviceExpirationDays).orElse(TRUSTED_DEVICE_EXPIRATION_DAYS);

        Basic2FASettings b2s = settings.getBasic2FASettings(); 
        minCreds2FA = b2s.getMinCreds();
        autoEnable = b2s.isAutoEnable();
        hideSwitch = !b2s.isAllowSelfEnableDisable();
        selectPreferred = b2s.isAllowSelectPreferred();

        enforcementPolicies = settings.getEnforcement2FA().stream().map(EnforcementPolicy::toString).collect(Collectors.toSet());
        logger.trace("Minimum creds for 2FA: {}", minCreds2FA);
        logger.trace("Current enforcement policies: {}", enforcementPolicies.toString());

        if (minCredsList == null) {
            minCredsList = new ArrayList<>();
            for (int i = BOUNDS_MINCREDS_2FA.getX(); i <= BOUNDS_MINCREDS_2FA.getY(); i++) {
                minCredsList.add(i);
            }
        }

    }

    private void promptBefore2FAProceed(String message, int newval) {

        Messagebox.show(message, null, Messagebox.YES | Messagebox.NO, Messagebox.EXCLAMATION,
                event -> {
                    if (Messagebox.ON_YES.equals(event.getName())) {
                        processUpdate(newval);
                    } else {  //Revert to last known working (or accepted)
                        reloadConfig();
                        BindUtils.postNotifyChange(StrongAuthViewModel.this, "minCreds2FA", "enforcementPolicies");
                    }
                }
        );

    }

    private void processUpdate(int newval) {

        if (locationExpiration > 0 && deviceExpiration > 0) {
            logger.trace("Updating settings");
            update2FASettings(newval, enforcementPolicies.stream().map(EnforcementPolicy::valueOf).collect(Collectors.toList()));
            reloadConfig();
        } else {
            UIUtils.showMessageUI(false, Labels.getLabel("adm.strongauth_exp_invalid"));
        }

    }

    private void update2FASettings(int minCreds, List<EnforcementPolicy> policies) {

        TrustedDevicesSettings tsettings = new TrustedDevicesSettings();
        tsettings.setDeviceExpirationDays(deviceExpiration);
        tsettings.setLocationExpirationDays(locationExpiration);

        settings.setTrustedDevicesSettings(tsettings);
        settings.setEnforcement2FA(policies);
        
        Basic2FASettings b2s = settings.getBasic2FASettings();
        b2s.setMinCreds(minCreds);
        b2s.setAutoEnable(autoEnable);
        b2s.setAllowSelfEnableDisable(!hideSwitch);
        b2s.setAllowSelectPreferred(selectPreferred);

        updateMainSettings(Labels.getLabel("adm.methods_change_success"));

    }

    private boolean updateMainSettings(String sucessMessage) {

        boolean success = false;
        try {
            logger.info("Updating global configuration settings");
            //update app-level config and persist
            settingsHandler.setSettings(settings);
            settingsHandler.save();
            Messagebox.show(sucessMessage, null, Messagebox.OK, Messagebox.INFORMATION);
            success = true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            UIUtils.showMessageUI(false, Labels.getLabel("adm.conffile_error_update"));
        }

        logActionDetails(Labels.getLabel("adm.strongauth_action"), success);
        return success;

    }
    
    void logActionDetails(String action, boolean success) {        
        logger.debug("{}: result {} - performed by user '{}'", action,
                success ? "OK" : "FAILED", user.getUserName());
    }

}
