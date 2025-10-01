package io.jans.casa.plugins.acctlinking;

import io.jans.casa.extension.navigation.MenuType;
import io.jans.casa.extension.navigation.NavigationMenu;
import org.pf4j.Extension;

@Extension
public class AdminMenu implements NavigationMenu {

    public String getContentsUrl() {
        return "admin/menu.zul";
    }

    public MenuType menuType() {
        return MenuType.ADMIN_CONSOLE;
    }

    public float getPriority() {
        return 0.1f;
    }

}
