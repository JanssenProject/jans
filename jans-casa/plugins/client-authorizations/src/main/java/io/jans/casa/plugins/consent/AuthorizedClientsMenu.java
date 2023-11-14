package io.jans.casa.plugins.consent;

import io.jans.casa.extension.navigation.MenuType;
import io.jans.casa.extension.navigation.NavigationMenu;
import org.pf4j.Extension;

/**
 * Allows markup to be added to user navigation menu
 * @author jgomer
 */
@Extension
public class AuthorizedClientsMenu implements NavigationMenu {

    public String getContentsUrl() {
        return "menu.zul";
    }

    public MenuType menuType() {
        return MenuType.USER;
    }

    public float getPriority() {
        return 0.1f;
    }

}
