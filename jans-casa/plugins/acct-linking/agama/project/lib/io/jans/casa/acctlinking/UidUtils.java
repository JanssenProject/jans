package io.jans.casa.acctlinking;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.UserService;
import io.jans.service.cache.CacheProvider;
import io.jans.service.cdi.util.CdiUtil;

import java.io.IOException;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UidUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(UidUtils.class);
    
    public static String lookupUid(String uidRef, String uid, String extUid, String jansExtAttrName,
            String jansExtUid) throws IOException {

        if (uidRef == null) {
            //Find if the external account is already linked to a local one
            User user = CdiUtil.bean(UserService.class).getUserByAttribute(jansExtAttrName, jansExtUid, true);

            if (user == null) {
                boolean uidPassed = uid != null;

                if (uidPassed) {
                    logger.debug("Using uid passed: {}", uid);
                    return uid;
                }

                logger.info("Building a uid based on external id {}", extUid);
                return extUid + "-" + randSuffix(3);
            }

            logger.info("Using uid of the account already linked to {}", jansExtUid); 
            return user.getUserId();
        }
        
        logger.debug("Looking up uid ref {}", uidRef);
        Object value = CdiUtil.bean(CacheProvider.class).get(uidRef);
        if (value == null) throw new IOException("uid reference passed not found in Cache!");

        return value.toString();
        
    }
    
    public static List<String> attrValuesAdding(String uid, String attributeName, String valueToAdd) {
        
        User user = CdiUtil.bean(UserService.class).getUserByAttribute("uid", uid, false);
        if (user == null) return Collections.singletonList(valueToAdd);
        
        List<String> values = new ArrayList<>();
        List<String> currentValues = Optional.ofNullable(user.getAttributeValues(attributeName))
                .orElse(Collections.emptyList());
        values.addAll(currentValues);

        if (!currentValues.contains(valueToAdd)) {        
            values.add(valueToAdd);
        }
        return values;
        
    }
    
    public static String computeExtUid(String providerId, String id) {
        return providerId + ":" + id;
    }
    
    private Utils() { }

    // The idea here is to generate a random 3-char lengthed string "easy to remember"
    private static String randSuffix(int randSuffixLen) {
        
        String s = "";
        int radix = Math.min(15, Character.MAX_RADIX);  //radix 15 entails characters: 0-9 plus a-e
        
        for (int i = 0; i < randSuffixLen; i++) {
            long rnd = Math.random() * radix;     // rnd will belong to [0, radix - 1]                
            s += Integer.toString((int) rnd, radix);    // this adds a single character to s
        }
        return s;
    }
    
}
