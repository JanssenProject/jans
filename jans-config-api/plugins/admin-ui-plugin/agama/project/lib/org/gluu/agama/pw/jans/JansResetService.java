package org.gluu.agama.pw.jans;


import io.jans.as.common.model.common.User;
import io.jans.as.server.service.UserService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.orm.exception.operation.EntryNotFoundException;
import io.jans.service.MailService;
import io.jans.model.SmtpConfiguration;
import io.jans.util.StringHelper;
import io.jans.agama.engine.script.LogUtils;
import io.jans.as.common.service.common.ConfigurationService;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.regex.Pattern;

import org.gluu.agama.pw.ResetService;
import org.gluu.agama.pw.jans.EmailTemplate;
import org.gluu.agama.pw.jans.Labels;
import io.jans.service.net.NetworkService;
import jakarta.servlet.http.HttpServletRequest;

import io.jans.as.server.util.ServerUtil;

public class JansResetService extends ResetService{

    private String userPreferredLanguage;
    private static final String LANG = "lang";
    private static final String LOCALE = "locale";
    private static final String MAIL = "mail";
    private static final String UID = "uid";
    private static final String INUM_ATTR = "inum";
    private static final String PASSWORD = "userPassword";
    public static final String JANS_STATUS = "jansStatus";
    public static final String INACTIVE = "inactive";
    public static final String ACTIVE = "active";
    private static final int OTP_LENGTH = 6;
    private static final String SUBJECT_TEMPLATE = "Here's your verification code: %s";
    private static final String MSG_TEMPLATE_TEXT = "%s is the code to complete your verification";   
    private static final SecureRandom RAND = new SecureRandom();
    
    public JansResetService() {
    }

    @Override
    public Map<String, String> getUserEntityByMail(String email) {

        // SessionIdService sis = CdiUtil.bean(SessionIdService.class); 
        // sis.getSessionId(CdiUtil.bean(HttpServletRequest.class));

        ///
        LogUtils.log("Get User entity by mail");
        // HttpServletRequest req = CdiUtil.bean(NetworkService.class).getHttpServletRequest();

        HttpServletRequest req = CdiUtil.bean(HttpServletRequest.class);
        LogUtils.log("REQ is  : %", req);

        String headerIp = req.getHeader("X-Forwarded-For");

        LogUtils.log("Header ip is  : %", headerIp);
        
        LogUtils.log("Remote address is  : %", req.getRemoteAddr());

        ServerUtil serverUtil = CdiUtil.bean(ServerUtil.class);

        String clientIp = serverUtil.getIpAddress(req);
        LogUtils.log("Client Ip address is : %", clientIp);
        /////

        User user = getUser(MAIL, email);
        boolean local = user != null;
        LogUtils.log("There is % local account for %", local ? "a" : "no", email);
    
        if (local) {            
            String uid = getSingleValuedAttr(user, UID);
            String inum = getSingleValuedAttr(user, INUM_ATTR);

            // Creating a truly modifiable map
            Map<String, String> userMap = new HashMap<>();
            userMap.put(UID, uid);
            userMap.put(INUM_ATTR, inum);
            userMap.put("email", email);
            
    
            return userMap;
        }

        return new HashMap<>();
    }    

    private String getSingleValuedAttr(User user, String attribute) {
        Object value = null;
        if (attribute.equals(UID)) {
            //user.getAttribute("uid", true, false) always returns null :(
            value = user.getUserId();
        } else {
            value = user.getAttribute(attribute, true, false);
        }
        return value == null ? null : value.toString();

    }    

    @Override
    public boolean passwordPolicyMatch(String userPassword) {
    // Regex Explanation:
    // - (?=.*[!-~&&[^ ]]) ensures at least one printable ASCII character except space (also helps exclude space)
    // - (?=.*[!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~]) ensures at least one special character
    // - (?=.*[A-Za-z]) ensures at least one letter
    // - (?=.*\\d) ensures at least one digit
    // - [!-~&&[^ ]] limits all characters to printable ASCII excluding space (ASCII 33â126)
    String regex = '''^(?=.*[A-Za-z])(?=.*\\d)(?=.*[!"#$%&'()*+,-./:;<=>?@[\\\\]^_`{|}~])[!-~&&[^ ]]{12,24}$''';
    Pattern pattern = Pattern.compile(regex);
    return pattern.matcher(userPassword).matches();
    }   

    @Override
    public String updateUserPassword(String userPassword, String mail) throws Exception {
        User user = getUser(MAIL, mail);
        user.setAttribute("userPassword", userPassword);

        UserService userService = CdiUtil.bean(UserService.class);
        user = userService.updateUser(user);
    
        if (user == null) {
            throw new EntryNotFoundException("Updated user not found");
        }
    
        return getSingleValuedAttr(user, INUM_ATTR);
    }     
    
    @Override
    public String sendEmail(String to) {
        String  userLang = null; 
        User user = getUser(MAIL, to);
        LogUtils.log("User is: %", user);
        // userLang= getSingleValuedAttr(user, LANG);
        // if (userLang == null || userLang.isBlank()){
        //     userLang = getSingleValuedAttr(user, LOCALE);
        // }
                        
        Map<String, String> labels = Labels.LANG_LABELS.getOrDefault("en", Labels.LANG_LABELS.get("en"));

        LogUtils.log("Final language used: %", labels != null ? userLang : "en");


        IntStream digits = RAND.ints(OTP_LENGTH, 0, 10);
        String otp = digits.mapToObj(i -> "" + i).collect(Collectors.joining());

        String subject = labels.get("subject");
        String msgText = labels.get("msgText").replace("{0}", otp);
        String line1 = labels.get("line1");
        String line2 = labels.get("line2");
        String line3 = labels.get("line3");
        String line4 = labels.get("line4");

        String htmlBody = EmailTemplate.get(otp, line1, line2, line3, line4); 

        SmtpConfiguration smtpConfiguration = getSmtpConfiguration();
        String from = smtpConfiguration.getFromEmailAddress();

        MailService mailService = CdiUtil.bean(MailService.class);
        if (mailService.sendMailSigned(from, from, to, null, subject, msgText, htmlBody)) {
            LogUtils.log("E-mail has been delivered to % with code %", to, otp);
            return otp;
        }

        LogUtils.log("E-mail delivery failed, check jans-auth logs");
        return null;
    } 

    private SmtpConfiguration getSmtpConfiguration() {
        ConfigurationService configurationService = CdiUtil.bean(ConfigurationService.class);
        SmtpConfiguration smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
        if (smtpConfiguration.getFromEmailAddress() == null || smtpConfiguration.getFromEmailAddress().isEmpty()) {
            LogUtils.log("Your smtp configuration not found, Please configure SMTP");
        } else {
            LogUtils.log("Your smtp configuration found");
        }
        
        return smtpConfiguration;

    } 
    
    private static User getUser(String attributeName, String value) {
        UserService userService = CdiUtil.bean(UserService.class);
        return userService.getUserByAttribute(attributeName, value, true);
    }       
    
}