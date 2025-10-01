package io.jans.casa.plugins.sampleauthn.vm;

import io.jans.casa.core.pojo.User;
import io.jans.casa.plugins.sampleauthn.service.SampleCredentialService;
import io.jans.casa.service.ISessionContext;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.util.resource.Labels;
import org.zkoss.zk.ui.select.annotation.WireVariable;
import org.zkoss.zul.Messagebox;
 
/**
 * ZK ViewModel associated to this plugin's cred-details.zul page. See the <code>viewModel</code> attribute in
 * panel component of <code>cred-details.zul</code>. In Casa, the MVVM (model-view-viewModel) design pattern is used 
 */
public class SampleCredentialVM {

    private Logger logger = LoggerFactory.getLogger(getClass());
    
    private String UNDEFINED_COLOR = "#FFFFFF";

    @WireVariable
    private ISessionContext sessionContext;

    private String userId;
    private SampleCredentialService credService;
    
    private String favoriteColor;

    //getter and setter are needed so the bind can occur from the template, see index.zul
    public String getFavoriteColor() {
        return favoriteColor;
    }
    
    public void setFavoriteColor(String favoriteColor) {
        this.favoriteColor = favoriteColor;
    }

    @Init
    public void init() {
        logger.info("ViewModel inited");
        userId = sessionContext.getLoggedUser().getId();        
        credService = SampleCredentialService.getInstance();
        favoriteColor = credService.getUserColor(userId);

        favoriteColor = Optional.ofNullable(favoriteColor).orElse(UNDEFINED_COLOR);
    }

    //Add more methods as required for the zul page to do what it is supposed to do
    //This requires knowledge of ZK framework. Existing pages and classes in Casa can be used as a guide

    public void update() {
        if (credService.storeUserColor(userId, favoriteColor)) {
            Messagebox.show(Labels.getLabel("sample.done"), null, Messagebox.OK, Messagebox.INFORMATION);
        }
        //You should handle the case of failure too
    }
    
}
