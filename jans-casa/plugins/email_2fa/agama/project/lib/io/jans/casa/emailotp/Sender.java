package io.jans.casa.emailotp;

import io.jans.agama.model.Flow;
import io.jans.service.MailService;
import io.jans.service.cdi.util.CdiUtil;

import jakarta.activation.*;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sender {

    private static final Logger logger = LoggerFactory.getLogger(Flow.class);
    private static final SecureRandom RAND = new SecureRandom();
    
    private int length;
    private int expirationMins;
    private String email;
    
    private String otp;
    private long otpIssuedAt;
    private String subject;
    private String message;
    
    //don't remove
    public Sender() { }
        
    public Sender(Map<String, Object> conf, String email) {
        
        length = grabInteger(conf, "otp_length", 6);
        expirationMins = grabInteger(conf, "otp_lifetime", 1);
        subject = Optional.ofNullable(conf).map(m -> m.get("subject"))
                .map(Object::toString).orElse("Your passcode is %");
        message = Optional.ofNullable(conf).map(m -> m.get("message"))
                .map(Object::toString).orElse("Your passcode is %");
        this.email = email;
        
    }
    
    public void send() {
        
        logger.info("Generating random OTP code of length {} and life time of {} minutes", length, expirationMins);
        IntStream digits = RAND.ints(length, 0, 10);
        otp = digits.mapToObj(i -> "" + i).collect(Collectors.joining());
        
        otpIssuedAt = System.currentTimeMillis();
        logger.debug("OTP is {}", otp);
      
        String msg = String.format(message, otp);
        MailService ms = CdiUtil.bean(MailService.class);
        
        if (!ms.sendMailSigned(null, null, email, email, String.format(subject, otp), msg, msg))
            throw new IOException("Failed to send e-mail");
  
    }
    
    public boolean matches(String code) {

        boolean m = code.equals(otp);
        logger.info("Code {}", m ? "entered matches" : "does not match");
        return m;

    }
    
    public boolean isOutOfTimeWindow() {
        return System.currentTimeMillis() - otpIssuedAt > TimeUnit.MINUTES.toMillis(expirationMins); 
    }
    
    private int grabInteger(Map<String, Object> map, String key, int defValue) {
        
        return Optional.ofNullable(map).map(m -> m.get(key)).map(obj -> {
                    try {
                        int val = Integer.class.cast(obj);
                        if (val <= 0) throw new IllegalArgumentException();
                    } catch (Exception e) {
                        logger.error("Error converting {} to a positive number. Using a default value", obj.toString());
                    }
            }).orElse(defValue);
            
    }

}
