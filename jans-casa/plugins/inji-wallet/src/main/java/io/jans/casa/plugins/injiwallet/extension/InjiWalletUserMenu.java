package io.jans.casa.plugins.injiwallet.extension;

import io.jans.casa.extension.navigation.MenuType;
import io.jans.casa.extension.navigation.NavigationMenu;

import org.pf4j.Extension;

/**
 * Extension that adds Inji Wallet to the Casa user menu
 * Provides user interface for managing Inji Wallet credentials
 */
@Extension
public class InjiWalletUserMenu implements NavigationMenu {

    public String getContentsUrl() {
        return "user/menu.zul";
    }

    public MenuType menuType() {
        return MenuType.USER;
    }

    public float getPriority() {
        return 0.5f;
    }

}