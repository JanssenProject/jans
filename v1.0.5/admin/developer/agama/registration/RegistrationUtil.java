package io.jans.agama.samples;

import io.jans.agama.engine.script.LogUtils;
import io.jans.as.common.model.common.User;
import io.jans.as.server.service.UserService;
import io.jans.service.cdi.util.CdiUtil;

import java.util.Date;

public class RegistrationUtil {
    
    public static String register(String givenName, String userName, String password, String password2, String email) {
        
        UserService uss = CdiUtil.bean(UserService.class);
        User user = uss.getUser(userName, "uid");
        
        if (user != null) return "This account already exists";
        
        if (!email.matches(".+@.+")) return "Invalid e-mail address";
        
        if (!password2.equals(password)) return "Passwords do not match";
        
        user = new User();
        user.setUserId(userName);
        user.setAttribute("givenName", givenName);
        user.setAttribute("userPassword", password);
        user.setAttribute("mail", email);
        
        try {
            uss.addUser(user, true);
        } catch (Exception e) {
            LogUtils.log("@e ", e.getMessage());
            return "An unexpected error occurred";
        }
        return null;
    
    }
}