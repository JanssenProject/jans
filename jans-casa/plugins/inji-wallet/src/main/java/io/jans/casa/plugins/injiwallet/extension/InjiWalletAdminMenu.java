package io.jans.casa.plugins.injiwallet.extension;

import io.jans.casa.extension.navigation.MenuType;
import io.jans.casa.extension.navigation.NavigationMenu;

import org.pf4j.Extension;

/**
 * Extension that adds Inji Wallet to the Casa admin menu
 * Provides configuration interface for trusted credentials and registration settings
 */
@Extension
public class InjiWalletAdminMenu implements NavigationMenu {

    public String getContentsUrl() {
        return "admin/menu.zul";
    }

    public MenuType menuType() {
        return MenuType.ADMIN_CONSOLE;
    }

    public float getPriority() {
        return 0.5f;
    }

}