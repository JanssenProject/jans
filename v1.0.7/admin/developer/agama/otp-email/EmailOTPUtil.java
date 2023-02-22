package io.jans.agama.samples;

import io.jans.as.common.service.common.UserService;
import io.jans.service.cdi.util.CdiUtil;
import io.jans.service.MailService;

import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class EmailOTPUtil {
    
    private static final int OTP_LENGTH = 6;
    private static final String SUBJECT = "Your passcode to get access";
    private static final String BODY_TEMPLATE = "Hi, the code to complete your authentication is %s";
    private static final SecureRandom RAND = new SecureRandom();

    public static String send(String to) {
        
        IntStream digits = RAND.ints(OTP_LENGTH, 0, 10);
        String otp = digits.mapToObj(i -> "" + i).collect(Collectors.joining());
        
        String body = String.format(BODY_TEMPLATE, otp);
        MailService mailService = CdiUtil.bean(MailService.class);
        return mailService.sendMail(to, SUBJECT, body) ? otp : null;

    }
    
    public static String emailOf(String username) {
        UserService userService = CdiUtil.bean(UserService.class);
        return userService.getUser(username).getAttribute("mail");
    }
    
}