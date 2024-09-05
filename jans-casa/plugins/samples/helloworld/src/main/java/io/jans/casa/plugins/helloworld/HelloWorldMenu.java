package io.jans.casa.plugins.helloworld;

import io.jans.casa.extension.navigation.MenuType;
import io.jans.casa.extension.navigation.NavigationMenu;
import org.pf4j.Extension;

/**
 * Represents a menu item to be added to Casa navigation menu
 * @author jgomer
 */
@Extension
public class HelloWorldMenu implements NavigationMenu {

    public String getContentsUrl() {
        //Location of the template that holds the markup of the menu item to be added.
        //See the resource/assets directory
        return "menu.zul";
    }

    public MenuType menuType() {
        //Whether this menu item is to be added to the general user menu or the admin-only menu
        return MenuType.USER;
    }

    public float getPriority() {
        //A numeric value employed to sort the menu items. Items with higher priority appear
        //first in the menu
        return 0.5f;
    }

}
