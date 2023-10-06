package io.jans.casa.ui;

import io.jans.casa.core.ExtensionsManager;
import io.jans.casa.extension.navigation.MenuType;
import io.jans.casa.extension.navigation.NavigationMenu;
import org.slf4j.Logger;
import org.zkoss.util.Pair;
import org.zkoss.zk.ui.Execution;
import org.zkoss.zk.ui.Executions;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author jgomer
 */
@Named
@ApplicationScoped
public class MenuService {

    @Inject
    private Logger logger;

    @Inject
    private ExtensionsManager extManager;

    public List<Pair<String, NavigationMenu>> getMenusOfType(MenuType menuType) {

        Execution execution = Executions.getCurrent();

        return extManager.getPluginExtensionsForClass(NavigationMenu.class).stream()
                .filter(pair -> menuType.equals(pair.getY().menuType()))
                .map(pair -> new Pair<>(String.format("/%s/%s", ExtensionsManager.PLUGINS_EXTRACTION_DIR, pair.getX()), pair.getY()))
                .filter(pair -> {
                    //Implements check for issue #18
                    boolean validUrl = false;
                    String url = null;
                    try {
                        url = pair.getX() + "/" + pair.getY().getContentsUrl();
                        execution.getPageDefinition(url);
                        validUrl = true;
                    } catch (Exception e) {
                        logger.error("An error occurred when building fragment '{}': {}", url, e.getMessage());
                        logger.warn("This navigation menu will be skipped from page markup");
                    }
                    return validUrl;
                })
                .sorted(Comparator.comparing(pair -> -pair.getY().getPriority())).collect(Collectors.toList());

    }

}
