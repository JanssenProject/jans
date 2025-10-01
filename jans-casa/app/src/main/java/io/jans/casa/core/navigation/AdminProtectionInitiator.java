package io.jans.casa.core.navigation;

import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.util.Initiator;

import java.util.Map;

/**
 * Created by jgomer on 2019-03-19.
 */
public class AdminProtectionInitiator implements Initiator {

    @Override
    public void doInit(Page page, Map<String, Object> map) throws Exception {
        page.setAttribute("checkAdmin", "yes");
    }

}
