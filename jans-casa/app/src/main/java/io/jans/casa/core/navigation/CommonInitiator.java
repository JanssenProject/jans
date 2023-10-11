package io.jans.casa.core.navigation;

import io.jans.casa.core.ConfigurationHandler;
import io.jans.casa.misc.AppStateEnum;
import io.jans.casa.misc.Utils;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.util.Initiator;

import java.util.Map;

/**
 * This initiator can be used for zul pages which can be publicly available. It accounts for the application to have
 * finished loaded completely
 */
public class CommonInitiator implements Initiator {

    public void doInit(Page page, Map<String, Object> map) throws Exception {

        AppStateEnum state = Utils.managedBean(ConfigurationHandler.class).getAppState();
        state = state == null ? AppStateEnum.LOADING : state;
        String err = Labels.getLabel("general.error.general");

        switch (state) {
            case LOADING:
                setPageErrors(page, err, Labels.getLabel("general.app_starting"));
                break;
            case FAIL:
                setPageErrors(page, err, Labels.getLabel("general.app_not_started"));
                break;
        }

    }

    void setPageErrors(Page page, String error, String description) {
        page.setAttribute("error", error);
        page.setAttribute("description", description);
    }

}
