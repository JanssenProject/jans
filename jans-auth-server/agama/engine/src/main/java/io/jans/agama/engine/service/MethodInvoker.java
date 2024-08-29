package io.jans.agama.engine.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jans.agama.engine.misc.PrimitiveUtils;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.lang.reflect.*;
import java.util.*;
import java.util.AbstractMap.SimpleEntry;
import java.util.stream.*;

import org.slf4j.Logger;

@ApplicationScoped
public class MethodInvoker {
    
    @Inject
    private Logger logger;
    
    private ObjectMapper mapper;
        
    class LMethod {
        
        private Method m;
        private int level;
        
        LMethod(Method m, int level) {
            this.m = m;
            this.level = level;
        }
        
        int getLevel() {
            return level;
        }
        
        Method getMethod() {
            return m;
        }
        
    }
    
    enum paramTransformation { 
        IDENTITY(0),
        PRIMITIVE_NUMBER(1), 
        SINGLE_CHAR(1),
        MAPPER(4);
    
        int weight;     //Measures an object conversion effort
        
        paramTransformation(int weight) {
            this.weight = weight;
        }
        
        static int score(List<paramTransformation> trs) {            
            return trs.stream().mapToInt(t -> t.weight).sum();
        }
        
    }
    
    public Object callConstructor(Class cls, Object args[]) throws Exception {

        int arity = args.length;
        
        PriorityQueue<SimpleEntry<Constructor, List<paramTransformation>>> pq = 
                new PriorityQueue<>(Comparator.comparing(se -> paramTransformation.score(se.getValue())));

        Stream.of(cls.getConstructors()).filter(cons -> acceptableExecutable(cons, arity, false))
                .map(cons -> new SimpleEntry<>(cons, argsTransformations(cons, args)))
                .filter(se -> se.getValue() != null).forEach(pq::add);
                
        int attempts = 0;
        SimpleEntry<Constructor, List<paramTransformation>> entry = pq.poll();

        while (entry != null) {

            try {
                List<paramTransformation> trs = entry.getValue();
                Constructor cons = entry.getKey();

                if (logger.isDebugEnabled()) {
                    logger.debug("Trying to create an instance using constructor: {}", cons.toGenericString());
                }
                
                attempts++;
                return cons.newInstance(applyArgsTransformations(cons.getParameters(), args, trs));                
            } catch (InvocationTargetException e) {     //the call succeeded but the constructor threw error
                //"return" the "real" exception, e is just a wrapper here
                throw (Exception) e.getCause();
            } catch (Exception e) {
                logger.error("", e);
                //Continue trying with the best remaining constructor in the queue
            }
            entry = pq.poll();
        }

        throw new InstantiationException(String.format("Unable to find a suitable constructor with " + 
                "arity %d in class %s - %d attempts made", arity, cls.getName(), attempts));
      
    }
    
    public Object call(Class cls, Object instance, String methodName, Object args[]) throws Exception {

        boolean noInst = instance == null;      //true if it's a static method call
        int arity = args.length;
        
        PriorityQueue<SimpleEntry<LMethod, List<paramTransformation>>> pq = new PriorityQueue<>(
                Comparator.comparing(se -> se.getKey().getLevel() + paramTransformation.score(se.getValue())));

        candidateMethodEntries(cls, methodName, arity, noInst, args).forEach(pq::add);
                
        int attempts = 0;
        SimpleEntry<LMethod, List<paramTransformation>> entry = pq.poll();
        
        while (entry != null) {

            try {
                List<paramTransformation> trs = entry.getValue();
                Method method = entry.getKey().getMethod();

                if (logger.isDebugEnabled()) {
                    logger.debug("Trying to invoke method: {}", method.toGenericString());
                }
            
                attempts++;
                return method.invoke(instance, applyArgsTransformations(method.getParameters(), args, trs));
            } catch (InvocationTargetException e) {     //the call succeeded but the method threw error
                //"return" the "real" exception, e is just a wrapper here
                throw (Exception) e.getCause();
                
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
                //Continue trying with the best remaining method in the queue
            }
            entry = pq.poll();
        }

        throw new NoSuchMethodException(String.format("Unable to find a suitable method %s with arity %d" + 
                " in class %s - %d attempts made", methodName, arity, cls.getName(), attempts));

    }
    
    private List<SimpleEntry<LMethod, List<paramTransformation>>> candidateMethodEntries(
            Class cls, String methodName, int arity, boolean isStatic, Object[] args) {

    /*
    The Class passed is scanned in search for methods that comply the arity, name, and staticness
    requirements. For every match, an analysis of arguments vs. method parameters is made to assess
    the effort needed to transform the arguments so that they fit the data types associated to the
    parameters. The scan is applied to the whole parent class hierarchy as well
    While something similar could have been achieved by a single call to Class#getMethods, the 
    getDeclaredMethods was preferred because when there are methods with the same signature (name, 
    parameter types) and return type, getMethods only returns the most specific method (check JDK
    docs). This is not always desirable because such method may be inaccessible due to language
    access control (see jans#7064). In this case, a method somewhere up in the class hierarchy might
    be of use instead  
    The effort needed to convert a given argument to a parameter is based on the weights found in
    the paramTransformation enumeration (at the top of this file). Note the usage of the score
    function in the Comparator created in method call. Also notice the usage of an additive factor 
    that depends on how far the method chosen is from the class where the search was started. This
    introduces a bias in favor of methods belonging to classes "closer" to the starting class. This
    feels a pretty natural way to search for the best suitable method
    When the Class passed actually represents an interface, there is no need to traverse the hierarchy
    because if an interface A extends another interface B, there is no way in Java to reference the
    (static) methods in B using a syntax like A.method(...) - only B.method(...) compiles effectively    
    */
        int l = 0;
        Class current = cls;
        List<SimpleEntry<LMethod, List<paramTransformation>>> entries = new ArrayList<>();

        while (current != null) {            
            logger.debug("Looking up candidate methods in class {}", current.getName());
            
            for (Method m : current.getDeclaredMethods()) {
                if (m.getName().equals(methodName) && acceptableExecutable(m, arity, isStatic)) {
                    List<paramTransformation> ptrs = argsTransformations(m, args);

                    if (ptrs != null) {
                        entries.add(new SimpleEntry<>(new LMethod(m, l), ptrs)); 
                    }
                }
            }
            //if current is an interface, the call below evaluates null
            current = current.getSuperclass();
            l++;
        }
        return entries;
                
    }
    
    private Object[] applyArgsTransformations(Parameter[] parameters, Object arguments[],
            List<paramTransformation> trs) throws IllegalArgumentException {

        if (parameters.length > 0) {
            logger.debug("Using transformation of params: {}", trs);
        }

        Object[] javaArgs = new Object[parameters.length];
        int i = -1;
            
        for (Parameter p : parameters) {
            Object arg = arguments[++i];
            Class<?> argClass = arg == null ? null : arg.getClass();

            Class<?> paramType = p.getType();
            String typeName = paramType.getName();
            
            paramTransformation ts = trs.get(i);
            Object javaArg = null;
            
            switch (ts) {
                case IDENTITY:
                    javaArg = arg;
                    break;

                case PRIMITIVE_NUMBER:
                    javaArg = PrimitiveUtils.primitiveNumberFrom((Number) arg, paramType);
                    
                    if (javaArg == null)
                        throw new IllegalArgumentException(String.format(
                                "Cannot convert argument of class %s to a %s",
                                argClass.getName(), typeName));

                    break;
                    
                case SINGLE_CHAR:
                    String s = arg.toString();
                    int len = s.length();

                    if (len == 0 || len > 1)
                        throw new IllegalArgumentException(String.format(
                                "Cannot convert argument of class %s to a %s. Length is not 1",
                                argClass.getName(), typeName));
                    
                    javaArg = s.charAt(0);

                    break;
                    
                case MAPPER:
                    try {
                        Type parameterizedType = p.getParameterizedType();
                        String ptypeName = parameterizedType.getTypeName();
                        logger.debug("Parsing argument of class {} to {} with Object Mapper",
                                argClass.getCanonicalName(), ptypeName);
                        
                        JavaType javaType = mapper.getTypeFactory().constructType(parameterizedType);
                        javaArg = mapper.convertValue(arg, javaType);
                    } catch (IllegalArgumentException ie) {
                        throw ie;
                    } catch (Exception e) {
                        throw new IllegalArgumentException(e);
                    }

                    break;
                    
                default:
                    throw new IllegalArgumentException(String.format(
                        "Cannot handle parameter transformation %s, index %d", ts.toString(), i));
            }
            javaArgs[i] = javaArg;
        }
        return javaArgs;

    }
    
    private List<paramTransformation> argsTransformations(Executable javaExec, Object arguments[]) {

        List<paramTransformation> trans = new ArrayList<>();
        int i = -1;
        logger.debug("Computing param transformations for executable {}", javaExec.toGenericString());
        
        for (Parameter p : javaExec.getParameters()) {            
            Object arg = arguments[++i];
            logger.trace("Examining argument at index {}", i);
            
            Class<?> paramType = p.getType();
            String typeName = paramType.getName();
            
            if (arg == null) {

                if (PrimitiveUtils.isPrimitive(paramType, false)) {
                    logger.trace("null value passed for a primitive parameter of type {}", typeName);
                    return null;
                }

                trans.add(paramTransformation.IDENTITY);
                continue;
            }
            if (typeName.equals(Object.class.getName())) {  //This parameter can receive anything :(
                logParamIs(typeName);
                trans.add(paramTransformation.IDENTITY);
                continue;
            }
            
            //argClass does not carry type information due to Java type erasure
            Class<?> argClass = arg.getClass();

            //Try the cheaper compatibility checks first
            //Note: A numeric literal coming from Javascript code lands as a Double
            Boolean primCompat = PrimitiveUtils.compatible(argClass, paramType);
            if (primCompat != null) {

                if (primCompat) {
                    logger.trace("Parameter is a primitive (or wrapped) {}", typeName);
                    trans.add(paramTransformation.IDENTITY);

                } else if (Number.class.isAssignableFrom(argClass)) {
                    logger.trace("Parameter is a primitive (or wrapped) {}", typeName);
                    trans.add(paramTransformation.PRIMITIVE_NUMBER);
                    
                } else {
                    logMismatch(argClass, typeName);
                    return null;
                }
                
            } else if (CharSequence.class.isAssignableFrom(argClass)) {

                primCompat = PrimitiveUtils.compatible(Character.class, paramType);

                if (Optional.ofNullable(primCompat).orElse(false)) {
                    logParamIs(typeName);
                    trans.add(paramTransformation.SINGLE_CHAR);

                } else if (paramType.isAssignableFrom(argClass)) {
                    logParamIs(typeName);
                    trans.add(paramTransformation.IDENTITY);
                
                } else if (paramType.equals(Character[].class) || paramType.equals(char[].class)) {
                    logParamIs(typeName);
                    trans.add(paramTransformation.MAPPER);

                } else {
                    logMismatch(argClass, typeName);
                    return null;
                }

            } else {
                //argClass should be NativeArray or NativeObject if the value was not created/derived
                //from a Java call
                Type parameterizedType = p.getParameterizedType();
                String ptypeName = parameterizedType.getTypeName();

                boolean straight = false;
                if (paramType.isInstance(arg)) {
                    //ptypeName and typeName are equal when there is no type information in the parameter
                    //(method signature). For instance: String[], List, MyBean (no parameterized types)
                    straight = ptypeName.equals(typeName);

                    if (!straight && ParameterizedType.class.isInstance(parameterizedType)) {
                        //make straight assignment if all type params are like <?,?,...>
                        straight = Stream.of(((ParameterizedType) parameterizedType).getActualTypeArguments())
                                .map(Type::getTypeName).allMatch("?"::equals);
                    }
                }

                logParamIs(ptypeName);
                trans.add(straight ? paramTransformation.IDENTITY: paramTransformation.MAPPER);
            }
        }
        return trans;

    }

    private void logMismatch(Class<?> argClass, String typeName) {
        logger.trace("{} passed for a {}", argClass.getSimpleName(), typeName);
    }
    
    private void logParamIs(String typeName) {
        logger.trace("Parameter is a {}", typeName);
    }
    
    private boolean acceptableExecutable(Executable e, int arity, boolean staticRequired) {

        int mod = e.getModifiers();
        return e.getParameterCount() == arity && Modifier.isPublic(mod) && 
                (staticRequired ? Modifier.isStatic(mod) : true);

    }
    
    @PostConstruct
    private void init() {
        mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }
    
}
