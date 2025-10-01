package io.jans.casa.ui.vm.user;

import io.jans.casa.core.*;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.extension.PreferredMethodFragment;
import io.jans.casa.misc.Utils;
import io.jans.casa.ui.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zk.ui.select.annotation.WireVariable;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is the ViewModel of page fragment preferred.zul (and the fragments included by it). It controls the functionality
 * for setting the user's preferred authentication method when second factor authentication is enabled
 */
public class UserPreferenceViewModel extends UserViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable("extensionsManager")
    private ExtensionsManager extManager;

    private List<Pair<String, String>> preferredFragments;
    private boolean mfaEnabled;
    private boolean uiNotEnoughCredsFor2FA;
    
    private boolean uiPreferredWindowShown;
    private List<Pair<String, String>> availMethods;
    private String selectedMethod;

    public int getMinCredsFor2FA() {
        return confSettings.getBasic2FASettings().getMinCreds();
    }

    public boolean isEnableDisableAllowed() {
        return confSettings.getBasic2FASettings().isAllowSelfEnableDisable();
    }

    public boolean isUiNotEnoughCredsFor2FA() {
        return uiNotEnoughCredsFor2FA;
    }

    public boolean isMfaEnabled() {
        return mfaEnabled;
    }

    public void setMfaEnabled(boolean mfaEnabled) {
        this.mfaEnabled = mfaEnabled;
    }

    public List<Pair<String, String>> getPreferredFragments() {
        return preferredFragments;
    }

    public boolean isUiPreferredWindowShown() {
        return uiPreferredWindowShown;
    }
    
    public List<Pair<String, String>> getAvailMethods() {
        return availMethods;
    }
    
    public String getSelectedMethod() {
        return selectedMethod;
    }

    @Init(superclass = true)
    public void childInit() {

    	PersistenceService persistenceService = Utils.managedBean(PersistenceService.class);
        List<Pair<AuthnMethod, Integer>> userMethodsCount = userService.getUserMethodsCount(user.getId());
        int totalCreds = userMethodsCount.stream().mapToInt(Pair::getY).sum();
        logger.info("Number of credentials for user {}: {}", user.getUserName(), totalCreds);

        selectedMethod = user.getPreferredMethod();
    	mfaEnabled = selectedMethod != null;
    	
        if (mfaEnabled) {
        	//Checks if selectedMethod maps to a real acr, or is otherwise marker data
        	if (!confSettings.getAcrPluginMap().containsKey(selectedMethod)) {
        	    selectedMethod =  null; 
        	}
    	} else {
            //Try to autoenable 2FA. This covers the case in which admin sets the autoenable feature after users
            //have already enrolled creds in the system. Users will be prompted for 2FA the next time they login
    	    userService.attemptAutoEnable2FA(user, totalCreds);
    	}
                
        availMethods = new ArrayList<>();
        
    	if (userMethodsCount.size() > 1 && confSettings.getBasic2FASettings().isAllowSelectPreferred()) {
    		availMethods = userMethodsCount.stream().map(Pair::getX)
                .map(aMethod -> new Pair<>(aMethod.getAcr(), Labels.getLabel(aMethod.getUINameKey())))
                .collect(Collectors.toList()); 
    	}
        
        uiNotEnoughCredsFor2FA = totalCreds < getMinCredsFor2FA();

        preferredFragments = extManager.getPluginExtensionsForClass(PreferredMethodFragment.class).stream()
                .map(p -> new Pair<>(String.format("/%s/%s", ExtensionsManager.PLUGINS_EXTRACTION_DIR, p.getX()), p.getY().getUrl()))
                .collect(Collectors.toList());

    }

    public void change() {
        //If 2FA has been turned on, try to restore the preferred method if there was any set previously 
        boolean outcome = mfaEnabled ? userService.turn2faOn(user, selectedMethod) : userService.turn2faOff(user);
        UIUtils.showMessageUI(outcome);
    }
    
    @NotifyChange("uiPreferredWindowShown")
    public void prepareSelection() {
    	uiPreferredWindowShown = true;
    }

    @NotifyChange("uiPreferredWindowShown")
    public void cancelUpdate(Event event) {
        uiPreferredWindowShown = false;
        if (event != null && event.getName().equals(Events.ON_CLOSE)) {
            event.stopPropagation();
        }
    }
    
    public void changePreferred(String preferred) {
    	selectedMethod = preferred;
    }
    
    @NotifyChange("uiPreferredWindowShown")
    public void update() {
    	boolean outcome = userService.turn2faOn(user, selectedMethod);
    	UIUtils.showMessageUI(outcome);
    	cancelUpdate(null);
    }
    
}
