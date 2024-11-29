package io.jans.casa.plugins.emailotp.vm;

import io.jans.casa.core.pojo.User;
import io.jans.casa.plugins.emailotp.service.EmailOtpService;
import io.jans.casa.service.ISessionContext;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zkoss.bind.annotation.Init;
import org.zkoss.zk.ui.select.annotation.WireVariable;
 
public class EmailOtpVM {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @WireVariable
    private ISessionContext sessionContext;

    private String userId;
    private List<String> emails;
    
    public List<String> getEmails() {
        return emails;
    }
   
    @Init
    public void init() {
        logger.info("ViewModel inited");
        userId = sessionContext.getLoggedUser().getId();        
        emails = EmailOtpService.getInstance().emailsOf(userId);
    }
    
}
