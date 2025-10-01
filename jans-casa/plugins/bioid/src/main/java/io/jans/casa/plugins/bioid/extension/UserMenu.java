package io.jans.casa.plugins.bioid.extension;

import io.jans.casa.extension.navigation.MenuType;
import io.jans.casa.extension.navigation.NavigationMenu;
import org.pf4j.Extension;

/**
 * 
 * @author SafinWasi
 *
 */
@Extension
public class UserMenu implements NavigationMenu {

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
