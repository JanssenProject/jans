package io.jans.casa.extension.navigation;

/**
 * Enumerates the type of menus that can be extended in Gluu Casa, ie. menus to which items can be added via plugin
 * extensions.
 * @author jgomer
 */
public enum MenuType {
    /**
     * The menu shown in the administrator's dashboard.
     */
    ADMIN_CONSOLE,
    /**
     * The menu shown top-right (where "Help" and "Logout" are).
     */
    AUXILIARY,
    /**
     * The menu used by all users of the application (shown on the left of UI pages).
     */
    USER
}
