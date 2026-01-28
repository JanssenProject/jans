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
                "o",       //Organization Name
                "ou",     //Organizational Unit Name 
                "cn",     //Common Name
                "mail",   //e-mail
            };
            String uidAttrName = "mail";
            
            Map<String, String> profile = new HashMap<>();            
            profile.put("uid", attributes.get(uidAttrName));
                
            //Straight (one-to-one) copy of attributes from source to destination map
            attrNames.forEach(attr -> 
                profile.put(attr, attributes.get(attr))); 

            return profile;
        };
        
    private AttributeMappings() { }

}
