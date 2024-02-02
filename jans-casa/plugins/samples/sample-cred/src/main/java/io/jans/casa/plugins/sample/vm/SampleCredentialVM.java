package io.jans.casa.plugins.sample.vm;

import io.jans.casa.core.pojo.User;
import io.jans.casa.plugins.sample.service.SampleCredentialService;
import io.jans.casa.service.ISessionContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.select.annotation.WireVariable;
 
/**
 * ZK ViewModel associated to this plugin's cred-details.zul page. See the <code>viewModel</code> attribute in
 * panel component of <code>cred-details.zul</code>. In Casa, the MVVM (model-view-viewModel) design pattern is used 
 */
public class SampleCredentialVM {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable
    private ISessionContext sessionContext;

    private User user;
    private SampleCredentialService credService;

    @Init
    public void init() {
        logger.info("ViewModel inited");
        user = sessionContext.getLoggedUser();
        credService = SampleCredentialService.getInstance();
    }

    //Add more methods as required for the zul page to do what it is supposed to do
    //This requires knowledge of ZK framework. Existing pages and classes in Casa can be used as a guide

}
