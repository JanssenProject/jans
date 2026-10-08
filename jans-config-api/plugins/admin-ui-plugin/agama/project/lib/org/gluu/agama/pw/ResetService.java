package org.gluu.agama.pw;

import java.util.HashMap;
import java.util.Map;

import org.gluu.agama.pw.jans.JansResetService;

public abstract class ResetService {

    public abstract boolean passwordPolicyMatch(String userPassword);

    public abstract Map<String, String> getUserEntityByMail(String email);

    public abstract String sendEmail(String to);

    public abstract String updateUserPassword(String userPassword, String mail);   

    public static ResetService  getInstance(){
        return new JansResetService();
    }


}








