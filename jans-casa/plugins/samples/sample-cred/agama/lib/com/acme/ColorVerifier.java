package com.acme;

//import io.jans.orm.PersistenceEntryManager;
import io.jans.as.common.model.common.User;
import io.jans.as.server.service.UserService;
import io.jans.service.cdi.util.CdiUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ColorVerifier {
    
    private static Logger logger = LoggerFactory.getLogger(ColorVerifier.class);

    public static final String COLOR_ATTR = "secretAnswer";
    
    public static boolean matches(String inum, String color) {
        
        //PersistenceEntryManager entryManager = CdiUtil.bean(PersistenceEntryManager.class);
        logger.info("Retrieving info of user {}", inum); 
        User user = CdiUtil.bean(UserService.class).getUserByInum(inum, COLOR_ATTR);
        String userColor = user.getAttribute(COLOR_ATTR);
        
        logger.info("Checking if color {} matches {}", userColor, color);
        return Optional.ofNullable(userColor).map(color::equalsIgnoreCase).orElse(false);
        
    }
    
}
