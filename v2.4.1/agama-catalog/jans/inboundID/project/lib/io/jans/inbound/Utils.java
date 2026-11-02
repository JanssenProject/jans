package io.jans.inbound;

import io.jans.agama.engine.script.ScriptUtils;
import io.jans.util.Pair;

import java.lang.reflect.Field;

public class Utils {
    
    public static Field getMappingField(String fieldName)
            throws ClassNotFoundException, NoSuchFieldException, IllegalAccessException {
                
        int i = 0;
        boolean valid = fieldName != null;
        
        if (valid) {
            i = fieldName.lastIndexOf(".");
            valid = i > 0 && i < fieldName.length() - 1;
        }
        if (!valid)
            throw new IllegalAccessException("Unexpected value passed for mapping field: " + fieldName);
        
        String clsName = fieldName.substring(0, i);
        
        //This is a trick so the class that contains the mapping is effectively reloaded when the (agama)
        //project that holds the class is redeployed. While classes are normally reloaded in projects,
        //it is not the case here because the class name is not being passed in a Call directive. So the
        //following would not work:        
        //   cls =  CdiUtil.bean(ActionService.class).getClassLoader().loadClass(clsName)
        //instead, the below emulates a proper Call instruction: 
        Pair<Object, Exception> p = ScriptUtils.callAction(null, clsName, "class", null);
        Exception ex = p.getSecond(); 
        if (ex != null) throw new ClassNotFoundException("Unknown class " + clsName, ex);

        Class<?> cls = (Class<?>) p.getFirst();
        return cls.getDeclaredField(fieldName.substring(i + 1));
        
    }

    private Utils() { }

}
