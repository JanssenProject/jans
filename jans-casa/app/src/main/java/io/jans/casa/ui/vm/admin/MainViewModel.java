package io.jans.casa.ui.vm.admin;

import io.jans.casa.conf.MainSettings;
import io.jans.casa.core.ConfigurationHandler;
import io.jans.casa.extension.navigation.MenuType;
import io.jans.casa.extension.navigation.NavigationMenu;
import io.jans.casa.misc.Utils;
import io.jans.casa.ui.UIUtils;
import io.jans.casa.ui.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.Pair;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Messagebox;

import java.util.List;

/**
 * @author jgomer
 */
public class MainViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private static ConfigurationHandler confHandler;

    @WireVariable
    private MenuService menuService;

    private List<Pair<String, NavigationMenu>> extraButtons;

    public List<Pair<String, NavigationMenu>> getExtraButtons() {
        return extraButtons;
    }

    static {
        confHandler = Utils.managedBean(ConfigurationHandler.class);
    }

    @Init
    public void init() {
        extraButtons = menuService.getMenusOfType(MenuType.ADMIN_CONSOLE);
    }

    public MainSettings getSettings() {
        return confHandler.getSettings();
    }

    boolean updateMainSettings(String sucessMessage) {

        boolean success = false;
        try {
            logger.info("Updating global configuration settings");
            //update app-level config and persist
            confHandler.saveSettings();
            if (sucessMessage == null) {
                UIUtils.showMessageUI(true);
            } else {
                Messagebox.show(sucessMessage, null, Messagebox.OK, Messagebox.INFORMATION);
            }
            success = true;
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            UIUtils.showMessageUI(false, Labels.getLabel("adm.conffile_error_update"));
        }
        return success;

    }

    boolean updateMainSettings() {
        return updateMainSettings(null);
    }

}
