package io.jans.casa.ui.vm;

import io.jans.casa.core.AuthFlowContext;
import io.jans.casa.core.OIDCFlowService;
import io.jans.casa.core.SessionContext;
import io.jans.casa.extension.navigation.MenuType;
import io.jans.casa.extension.navigation.NavigationMenu;
import io.jans.casa.misc.WebUtils;
import io.jans.casa.ui.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.Pair;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.select.annotation.WireVariable;

import java.util.List;

/**
 * @author jgomer
 */
public class HeaderViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable("oIDCFlowService")
    private OIDCFlowService oidcFlowService;

    @WireVariable
    private AuthFlowContext authFlowContext;

    @WireVariable
    private MenuService menuService;

    @WireVariable
    private SessionContext sessionContext;

    private List<Pair<String, NavigationMenu>> contextMenuItems;

    public List<Pair<String, NavigationMenu>> getContextMenuItems() {
        return contextMenuItems;
    }

    @Init
    public void init() {
        contextMenuItems = menuService.getMenusOfType(MenuType.AUXILIARY);
    }

    public void logoutFromAuthzServer() {

        try {
            //When the session expires, the browser is automatically taken to /session-expired.zul (see zk.xml), so
            //in theory, the call below will not yield NPE, and authFlowContext.isHasSessionAtOP() is always true
            logger.trace("Log off attempt for {}", sessionContext.getUser().getUserName());

            //After End-User has logged out, the Client might request to log him out of the OP too
            //String idToken = authFlowContext.getIdToken();
            Executions.sendRedirect(oidcFlowService.getLogoutUrl());

            //Kill session
            WebUtils.invalidateSession(WebUtils.getServletRequest());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }

    }

}
