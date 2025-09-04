package io.jans.inbound;

import io.jans.as.common.model.common.User;
import io.jans.as.common.service.common.UserService;
import io.jans.orm.model.base.CustomObjectAttribute;
import io.jans.service.cdi.util.CdiUtil;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Map;
import java.util.function.UnaryOperator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IdentityProcessor {

    private Provider provider;
    private static Logger logger = LoggerFactory.getLogger(IdentityProcessor.class);
    
    public IdentityProcessor() { }
    
    public IdentityProcessor(Provider provider) {
        this.provider = provider;
    }
    
    public Map<String, List<Object>> applyMapping(Map<String, Object> profile, Field f)
            throws IllegalAccessException {

        logger.debug("Retrieving value of field {}", f.getName());
        UnaryOperator<Map<String, Object>> op = (UnaryOperator<Map<String, Object>>) f.get(f.getDeclaringClass());
        
        logger.debug("Applying mapping to incoming profile");
        Map<String, Object> pr = op.apply(profile);
        Map<String, List<Object>> res = new HashMap<>();
        
        for (String key: pr.keySet()) {
            Object value = pr.get(key);
            
            if (key != null && value != null) {
                List<Object> newValue;
                
                if (value.getClass().isArray()) {
                    newValue = Arrays.asList(value);
                } else if (Collection.class.isInstance(value)) {
                    newValue = new ArrayList<>((Collection) value);
                } else {
                    newValue = Collections.singletonList(value);
                }
                res.put(key, newValue);
            }
        }
        return res;
        
    }

    public String process(Map<String, List<Object>> profile) {
        
        if (profile.isEmpty()) throw new IllegalArgumentException("Empty profile data");

        UserService userService = CdiUtil.bean(UserService.class);
        logger.info("User provisioning started");

        User user = null;
        boolean update = true;
        String uid = profile.get(Attrs.UID).get(0).toString();
        String email = Optional.ofNullable(profile.get(Attrs.MAIL)).orElse(Collections.emptyList())
                .stream().findFirst().map(Object::toString).orElse(null);
                
        if (email != null && !email.contains("@")) throw new IllegalArgumentException("Invalid e-mail " + email);

        Map<String, List<Object>> profile2 = new HashMap<>(profile);  //ensure it is modifiable
        if (provider.isEmailLinkingSafe() && email != null) {

            user = userService.getUserByAttribute(Attrs.MAIL, email);
            if (user != null) {
                logger.debug("Identity of incoming user is matched to existing {} by e-mail linking. " +
                    "Ignoring incoming uid {}", user.getUserId(), uid);
                
                uid = user.getUserId();
                profile2.remove(Attrs.UID);
            }
        }
        
        if (user == null) {
            logger.debug("Retrieving user identified by {}", uid);
            user = userService.getUser(uid);
            update = user != null;
        }
        
        if (update) {
            logger.info("Preparing update for user {}...", uid);
            
            if (provider.isSkipProfileUpdate()) {
                List<Object> jansExtUids = profile2.get("jansExtUid");

                if (jansExtUids == null) {
                    profile2 = null;
                    logger.info("No attributes will be updated");
                } else {
                    profile2 = Collections.singletonMap("jansExtUid", jansExtUids);
                    logger.info("Only 'jansExtUid' will be updated");
                }
            }

            if (profile2 != null) {
                user.setCustomAttributes(attributesForUpdate(
                        user.getCustomAttributes(), profile2, provider.isCumulativeUpdate()));
                //ugly hack
                Optional.ofNullable(user.getAttributeValues("jansExtUid"))
                            .map(l -> l.toArray(new String[0])).ifPresent(user::setExternalUid);
    
                userService.updateUser(user);
            }
            
        } else {
            logger.info("Adding user {}", uid);
            
            user = new User();
            user.setCustomAttributes(attributesForAdd(profile));            
            userService.addUser(user, true);            
        }

        return uid;
        
    }
    
    private List<CustomObjectAttribute> attributesForUpdate(List<CustomObjectAttribute> customAttributes,
            Map<String, List<Object>> profile, boolean cumulative) {

        //Merge existing data of user plus incoming data in profile
        List<CustomObjectAttribute> customAttrs = new ArrayList<>(customAttributes);

        for (CustomObjectAttribute coa : customAttrs) {
            String attrName = coa.getName();
            List<Object> newValues = profile.get(attrName);

            if (newValues != null) {
                List<Object> values = new ArrayList<>(cumulative ? coa.getValues() : Collections.emptyList());
                newValues.stream().filter(nv -> !values.contains(nv)).forEach(values::add);

                profile.remove(attrName);
                coa.setValues(values);
            }
        }

        profile.forEach((k, v) -> {
                CustomObjectAttribute coa = new CustomObjectAttribute(k);
                coa.setValues(v);
                customAttrs.add(coa);
        });

        logger.trace("Resulting list of attributes:\n{}", customAttrs.toString());
        return customAttrs;

    }
    
    private List<CustomObjectAttribute> attributesForAdd(Map<String, List<Object>> profile) {
        
        List<CustomObjectAttribute> attrs = new ArrayList<>();        
        profile.forEach((k, v) -> {
                CustomObjectAttribute coa = new CustomObjectAttribute(k);
                coa.setValues(v);
                attrs.add(coa);
        });
        
        return attrs;

    }

}
