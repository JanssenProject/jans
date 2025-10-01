package io.jans.casa.ui.vm.admin;

import io.jans.casa.misc.Utils;
import io.jans.casa.ui.UIUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.bind.annotation.NotifyChange;
import org.zkoss.util.resource.Labels;

import java.util.List;

public class CORSViewModel extends MainViewModel {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private String origin;
    private List<String> origins;

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public List<String> getOrigins() {
        return origins;
    }

    @Init(superclass = true)
    public void childInit() {
        //Obtain a reference to the list
        origins = getSettings().getCorsDomains();
    }

    @NotifyChange("origins")
    public void dropOrigin(String orig) {
        origins.remove(orig);
        getSettings().setCorsDomains(origins);
        updateMainSettings(Labels.getLabel("adm.cors_action"));
    }

    @NotifyChange({"origins", "origin"})
    public void addOrigin() {

        if (Utils.isValidUrl(origin)) {
            if (!origins.contains(origin.toLowerCase())) {
                origins.add(origin.toLowerCase());
                
                getSettings().setCorsDomains(origins);
                if (updateMainSettings(Labels.getLabel("adm.cors_action"))) {
                    origin = "";
                }
            }
        } else {
            UIUtils.showMessageUI(false, Labels.getLabel("amd.cors_invalid_origin", new String[]{ origin }));
        }

    }

}
