package io.jans.casa.plugins.sample;

import io.jans.casa.extension.navigation.MenuType;
import io.jans.casa.extension.navigation.NavigationMenu;
import org.pf4j.Extension;

/**
 * An extension class implementing the {@link NavigationMenu} extension point.
 * @author jgomer
 */
@Extension
public class SampleMenu implements NavigationMenu {

    public String getContentsUrl() {
        return "menu.zul";
    }

    public MenuType menuType() {
        return MenuType.ADMIN_CONSOLE;
    }

    public float getPriority() {
        return 0.5f;
    }

}
