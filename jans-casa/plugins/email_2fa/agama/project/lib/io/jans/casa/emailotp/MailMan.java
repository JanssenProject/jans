package io.jans.casa.emailotp;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.UserService;
import io.jans.service.cdi.util.CdiUtil;

import java.util.*;

public class MailMan {
    
    public List<String> emailsOf(String inum) {
        User user = CdiUtil.bean(UserService.class).getUserByInum(inum, "mail");
        return Optional.ofNullable(user.getAttributeValues("mail")).orElse(Collections.emptyList());
    }
    
    public List<String> mask(List<String> emails) {
        
        List<String> masked = new ArrayList<>();
        
        emails.forEach(str -> {
                int i = str.indexOf("@");
                if (i != -1) {
                    masked.add(maskStr(str.substring(0, i)) + str.substring(i));
                }
        });
        return masked;
        
    }
    
    private String maskStr(String str) {
        
        int len = str.length();
        if (len < 3) return "*".repeat(len);
        
        int l = Long.valueOf(Math.round(len / 4.0)).intValue();
        String gap = "*".repeat(len / 2);
        return str.substring(0, l) + gap + str.substring(l + gap.length());
        
    }

}
