package io.jans.agama.samples

import io.jans.as.common.service.common.UserService
import io.jans.service.cdi.util.CdiUtil
import io.jans.service.MailService

import java.security.SecureRandom
import java.util.stream.Collectors
import java.util.stream.IntStream

class EmailOTPUtil {
    
    static final int OTP_LENGTH = 6
    static final String SUBJECT = "Your passcode to get access"
    static final String BODY_TEMPLATE = "Hi, the code to complete your authentication is %s"
    static final SecureRandom RAND = new SecureRandom()
    
    static String send(String to) {
        
        IntStream digits = RAND.ints(OTP_LENGTH, 0, 10)
        String otp = digits.mapToObj(i -> "" + i).collect(Collectors.joining())
        
        String body = String.format(BODY_TEMPLATE, otp)
        MailService mailService = CdiUtil.bean(MailService)
        mailService.sendMail(to, SUBJECT, body) ? otp : null

    }
    
    static String emailOf(String username) {
        UserService userService = CdiUtil.bean(UserService)
        userService.getUser(username).getAttribute("mail")
    }

}
