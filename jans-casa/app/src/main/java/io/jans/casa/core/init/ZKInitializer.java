package io.jans.casa.core.init;

import io.jans.casa.core.ZKService;
import io.jans.casa.misc.Utils;
import org.zkoss.zk.ui.WebApp;
import org.zkoss.zk.ui.util.WebAppInit;

/**
 * @author jgomer
 */
public class ZKInitializer implements WebAppInit {

    public void init(WebApp webApp) throws Exception {
        Utils.managedBean(ZKService.class).init(webApp);
    }

}
