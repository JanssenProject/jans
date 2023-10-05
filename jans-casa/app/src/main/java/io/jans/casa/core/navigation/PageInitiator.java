package io.jans.casa.core.navigation;

import io.jans.casa.core.SessionContext;
import io.jans.casa.core.pojo.User;
import io.jans.casa.misc.Utils;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.util.Initiator;

import java.util.Map;

/**
 * This initiator helps implementing basic protection: if no user is in session, pages will render error message, see
 * template general.zul
 * @author jgomer
 */
public class PageInitiator extends CommonInitiator implements Initiator {

    @Override
    public void doInit(Page page, Map<String, Object> map) throws Exception {

        User user = Utils.managedBean(SessionContext.class).getUser();
        if (user == null || (page.getAttribute("checkAdmin") != null && !user.isAdmin())) {
            setPageErrors(page, Labels.getLabel("usr.not_authorized"), null);
        }

    }

}
