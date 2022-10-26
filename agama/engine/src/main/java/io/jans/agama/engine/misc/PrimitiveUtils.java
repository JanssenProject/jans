package io.jans.agama.engine.misc;

import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class PrimitiveUtils {

    enum Primitive {
        CHARACTER(Character.class, Character.TYPE),
        BOOLEAN(Boolean.class, Boolean.TYPE),
        BYTE(Byte.class, Byte.TYPE),
        DOUBLE(Double.class, Double.TYPE),
        FLOAT(Float.class, Float.TYPE),
        INTEGER(Integer.class, Integer.TYPE),
        LONG(Long.class, Long.TYPE),
        SHORT(Short.class, Short.TYPE);
        
        private final Class<?> wrapperCls;
        private final Class<?> primitiveCls;
        
        private final static Map<Class<?>, Primitive> mapW = Arrays.stream(values())
                .collect(Collectors.toMap(p -> p.wrapperCls, p -> p));
        
        private final static Map<Class<?>, Primitive> mapP = Arrays.stream(values())
                .collect(Collectors.toMap(p -> p.primitiveCls, p -> p));
                
        Primitive(Class<?> wrapperCls, Class<?> primitiveCls) {
            this.wrapperCls = wrapperCls;
            this.primitiveCls = primitiveCls;
        }
        
        private static Primitive from(Map<Class<?>, Primitive> map, Class<?> cls) {
            return map.get(cls);
        }
        
        static Primitive fromWrapperClass(Class<?> cls) {
            return from(mapW, cls);
        }
        
        static Primitive fromPrimitiveClass(Class<?> cls) {
            return from(mapP, cls);
        }
        
        static Primitive fromWrapperOrPrimitiveClass(Class<?> cls) {
            return Optional.ofNullable(fromWrapperClass(cls)).orElse(fromPrimitiveClass(cls));
        }
        
    }
    
    public static Boolean compatible(Class<?> argumentCls, Class<?> paramType) {
        
        Primitive p = Primitive.fromWrapperClass(argumentCls);
        if (p != null) {
            if (argumentCls.equals(paramType)) return true;

            return p.equals(Primitive.fromPrimitiveClass(paramType));
        }
        return null;
    }
    
    public static boolean isPrimitive(Class<?> cls, boolean wrapperCounts) {
        Primitive p = wrapperCounts ? Primitive.fromWrapperOrPrimitiveClass(cls) : 
                Primitive.fromPrimitiveClass(cls);
        return p != null;
    }

    public static Object primitiveNumberFrom(Number value, Class destination) {
        
        Primitive prim = Primitive.fromWrapperOrPrimitiveClass(destination);
        if (prim != null) {
            switch (prim) {
                case BYTE:
                    return value.byteValue();
                case FLOAT:
                    return value.floatValue();
                case INTEGER:
                    return value.intValue();
                case LONG:
                    return value.longValue();
                case SHORT:
                    return value.shortValue();
            }
        }
        return null;

    }

}
