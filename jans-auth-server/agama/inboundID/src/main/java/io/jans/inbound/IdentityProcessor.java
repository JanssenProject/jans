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
    
    public IdentityProcessor() { }
    
    public IdentityProcessor(Provider provider) {
        this.provider = provider;
    }
    
    public Map<String, List<Object>> applyMapping(Map<String, Object> profile, ClassLoader classLoader)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        
        UnaryOperator<Map<String, Object>> op = getMapping(provider.getMappingClassField(), 
                classLoader == null ? getClass().getClassLoader() : classLoader);
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

        Logger logger = LoggerFactory.getLogger(getClass());
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

            if (provider.isSkipProfileUpdate()) {
                logger.info("Skipping profile update");
            } else {
                logger.info("Updating user {}", uid);
                user.setCustomAttributes(attributesForUpdate(
                        user.getCustomAttributes(), profile2, provider.isCumulativeUpdate()));
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
    
    private UnaryOperator<Map<String, Object>> getMapping(String field, ClassLoader clsLoader) 
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
        
        int i = 0;
        boolean valid = field != null;
        
        if (valid) {
            i = field.lastIndexOf(".");
            valid = i > 0 && i < field.length() - 1;
        }
        if (!valid) throw new IllegalAccessException("Unexpected value passed for mapping field: " + field);
        
        String clsName = field.substring(0, i);
        Class<?> cls = clsLoader.loadClass(clsName);
        Field f = cls.getDeclaredField(field.substring(i + 1));
        return (UnaryOperator<Map<String, Object>>) f.get(cls);

    }

}
