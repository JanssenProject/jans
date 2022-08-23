package io.jans.inbound;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

public class IdentityProcessor {

    private Provider provider;
    private UnaryOperator<Map<String, Object>> mapping;
    
    public IdentityProcessor(Provider provider, ClassLoader classLoader)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {

        this.provider = provider;
        this.mapping = getMapping(provider.getMappingClassField(), 
                classLoader == null ? getClass().getClassLoader() : classLoader);

    }
    
    public Map<String, List<Object>> applyMapping(Map<String, Object> profile) {
        
        Map<String, Object> pr = mapping.apply(profile);
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
    
    public String process(Map<String, List<?>> profile) {//throws Exception {
        //Provisions the user and returns its local id (inum)
        //reject if there are null values
        if (profile.isEmpty() && provider == null) return null;
        return "";
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
