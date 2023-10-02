package io.jans.casa.extension.navigation;

import org.pf4j.ExtensionPoint;

/**
 * A <a href="http://www.pf4j.org/" target="_blank">pf4j</a> extension point. Classes implementing this interface define
 * the characteristics of one or more navigation menu items that can be added to Gluu Casa UI.
 * @author jgomer
 */
public interface NavigationMenu extends ExtensionPoint {

    /**
     * Specifies a path that points to the actual contents of the menu items to be added to Casa UI. This can point to a
     * .zul, .jsp or static document relative to the base URL of the plugin to which the extension (that implements this
     * extension point) belongs to.
     * <p>As an example, if your <code>Plugin-Id</code> is <code>my-plugin</code> and you want to point to file
     * <code>assests/abc/menu.zul</code> of your maven project (which will be web-accessible via
     * <code>https://host/casa/pl/my-plugin/abc/menu.zul</code>), you'll just have to return <code>abc/menu.zul</code>.</p>
     * <p>For more information, visit plugin's developer guide at Jans Casa
     * <a href="#">docs</a></p>
     * @return The URL that serves the markup of this menu set.
     */
    String getContentsUrl();

    /**
     * Specifies the type of menu the extension deals with. This is a default interface method that returns always
     * <code>MenuType.USER</code>. Override this method if you want to return a different value
     * @return A menu type (not null).
     */
    default MenuType menuType() {
        return MenuType.USER;
    }

    /**
     * A value used to determine a relative order among extensions that implement this particular extension point. That is, it
     * determines which menus added by extensions are rendered first (top), second, etc. within the same class (eg. menus
     * targetted at regular users, or for admin dashboard). The first menu to be shown will be that with the greatest
     * priority value and the last menu included will correspond to the one with the lowest.
     * @return A priority value. It is recommended to use values in the interval [0,1].
     */
    float getPriority();

}
