package io.jans.casa.ui.vm.user;

import io.jans.casa.conf.MainSettings;
import io.jans.casa.core.*;
import io.jans.casa.core.pojo.User;
import io.jans.casa.extension.AuthnMethod;
import io.jans.casa.misc.Utils;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.select.annotation.WireVariable;

/**
 * This is the superclass of all ViewModels associated to zul pages used by regular users of the application
 * @author jgomer
 */
public class UserViewModel {

    @WireVariable
    protected SessionContext sessionContext;

    @WireVariable
    UserService userService;

    @WireVariable("passwordStatusService")
    private PasswordStatusService pst;

    MainSettings confSettings;

    User user;

    //This getter is used in several ZK pages like menubuttons.zul or user.zul
    public PasswordStatusService getPst() {
        return pst;
    }

    @Init
    public void init() {
        user = sessionContext.getUser();
        //Note MainSettings is not injectable in ViewModels
        confSettings = Utils.managedBean(ConfigurationHandler.class).getSettings();
        pst.reloadStatus();
    }

    public String getAuthnMethodPageUrl(AuthnMethod method) {

        String page = method.getPageUrl();
        String pluginId = confSettings.getAcrPluginMap().get(method.getAcr());
        if (pluginId != null) {
            page = String.format("/%s/%s/%s", ExtensionsManager.PLUGINS_EXTRACTION_DIR, pluginId, page);
        }
        return page;

    }

    int getScreenWidth() {
        return sessionContext.getScreenWidth();
    }

    Pair<String, String> getDeleteMessages(String nick, String extraMessage){

        StringBuilder text=new StringBuilder();
        if (extraMessage != null) {
            text.append(extraMessage).append("\n\n");
        }
        text.append(Labels.getLabel("usr.del_confirm", new String[]{ nick==null ? Labels.getLabel("general.no_named") : nick }));
        if (extraMessage != null) {
            text.append("\n");
        }

        return new Pair<>(Labels.getLabel("usr.del_title"), text.toString());

    }

    String resetPreferenceMessage(String credentialType, int nCredsOfType) {
        return userService.removalConflict(credentialType, nCredsOfType, user).getY();
    }

}
