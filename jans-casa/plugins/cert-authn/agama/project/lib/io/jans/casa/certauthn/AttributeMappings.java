package io.jans.casa.certauthn;

import java.util.function.UnaryOperator;
import java.util.*;

/**
 * Fields of this class can be referenced in the config properties of flow 
 * io.jans.casa.cert.oneStepAuthn, see mappingClassField
 */
public final class AttributeMappings {

    //Maps some "incoming attributes" in the certificate to build a user profile  
    public static final UnaryOperator<Map<String, String>> STRAIGHT = 

        attributes -> {
            String[] attrNames = new String[] {                
                "o",      //Organization Name
                "cn",     //Common Name
                "l",      //City
                "mail"    //e-mail
            };
            String attrForUid = "mail";
            
            Map<String, String> profile = new HashMap<>();            
            profile.put("uid", attributes.get(attrForUid));
                
            //Straight (one-to-one) copy of attributes from source to destination map
            List.of(attrNames).forEach(attr -> 
                profile.put(attr, attributes.get(attr))); 

            return profile;
        };
        
    private AttributeMappings() { }

}
