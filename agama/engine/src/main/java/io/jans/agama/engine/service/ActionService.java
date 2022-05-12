package io.jans.agama.engine.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import groovy.lang.GroovyClassLoader;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Stream;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import io.jans.agama.engine.misc.PrimitiveUtils;
import io.jans.agama.model.EngineConfig;

import org.slf4j.Logger;

@ApplicationScoped
public class ActionService {
    
    private static final String CLASS_SUFFIX = ".groovy";
    
    @Inject
    private Logger logger;
    
    @Inject
    private EngineConfig econf;
    
    private GroovyScriptEngine gse;
    private ObjectMapper mapper;
    private GroovyClassLoader loader;
    
    public Object callAction(Object instance, String className, String methodName, Object[] rhinoArgs)
            throws Exception {

        boolean noInst = instance == null;
        Class actionCls;
        
        if (!noInst) {
            actionCls = instance.getClass();
            className = actionCls.getName();
        } else {

            try {
                //logger.info("Using current classloader to load class " + className);
                //Try the fastest lookup first
                actionCls = Class.forName(className);
            } catch (ClassNotFoundException e) {
                try {
                    String classFilePath = className.replace('.', File.separatorChar) + CLASS_SUFFIX;
                    //GroovyScriptEngine classes are only really reloaded when the underlying file changes
                    actionCls = gse.loadScriptByName(classFilePath);
                } catch (ResourceException re) {
                    throw new ClassNotFoundException(re.getMessage(), e);
                }
            }
        }
        logger.info("Class {} loaded successfully", className);
        int arity = rhinoArgs.length;
        
        BiPredicate<Executable, Boolean> pr = (e, staticRequired) -> {
            int mod = e.getModifiers();
            return e.getParameterCount() == arity && Modifier.isPublic(mod) && 
                    (staticRequired ? Modifier.isStatic(mod) : true);
        };
        
        //Search for a method/constructor matching name and arity

        if (noInst && methodName.equals("new")) {
            Constructor constr = Stream.of(actionCls.getConstructors()).filter(c -> pr.test(c, false))
                    .findFirst().orElse(null);
            if (constr == null) {
                String msg = String.format("Unable to find a constructor with arity %d in class %s",
                        arity, className);
                logger.error(msg);
                throw new InstantiationException(msg);
            }

            logger.debug("Constructor found");
            Object[] args = getArgsForCall(constr, arity, rhinoArgs);

            logger.debug("Creating an instance");
            return constr.newInstance(args);
        }

        Method javaMethod = Stream.of(actionCls.getDeclaredMethods())
                .filter(m -> m.getName().equals(methodName)).filter(m -> pr.test(m, noInst))
                .findFirst().orElse(null);

        if (javaMethod == null) {
            String msg = String.format("Unable to find a method called %s with arity %d in class %s",
                    methodName, arity, className);
            logger.error(msg);
            throw new NoSuchMethodException(msg);
        }

        logger.debug("Found method {}", methodName);        
        Object[] args = getArgsForCall(javaMethod, arity, rhinoArgs);

        logger.debug("Performing method call");
        return javaMethod.invoke(instance, args);

    }
    
    private Object[] getArgsForCall(Executable javaExec, int arity, Object[] arguments)
            throws IllegalArgumentException {
        
        Object[] javaArgs = new Object[arity];
        int i = -1;
        
        for (Parameter p : javaExec.getParameters()) {
            Object arg = arguments[++i];
            Class<?> paramType = p.getType();
            String typeName = paramType.getName();
            logger.debug("Examining argument at index {}", i);
            
            if (arg == null) {
                logger.debug("Value is null");
                if (PrimitiveUtils.isPrimitive(paramType, false))
                    throw new IllegalArgumentException("null value passed for a primitive parameter of type "
                            + typeName);
                else continue;
            }
            if (typeName.equals(Object.class.getName())) {
                //This parameter can receive anything :(
                logger.trace("Parameter is a {}", typeName);
                javaArgs[i] = arg;
                continue;
            }
            
            Class<?> argClass = arg.getClass();

            //Try to apply cheaper conversions first (in comparison to mapper-based conversion)
            Boolean primCompat = PrimitiveUtils.compatible(argClass, paramType);
            if (primCompat != null) {

                if (primCompat) {
                    logger.trace("Parameter is a primitive (or wrapped) {}", typeName);
                    javaArgs[i] = arg;

                } else if (argClass.equals(Double.class)) {
                    //Any numeric literal coming from Javascript code lands as a Double
                    Object number = PrimitiveUtils.primitiveNumberFrom((Double) arg, paramType);
                    
                    if (number != null) {
                        logger.trace("Parameter is a primitive (or wrapped) {}", typeName);
                        javaArgs[i] = number;
                        
                    } else mismatchError(argClass, typeName);
                    
                } else mismatchError(argClass, typeName);
                
            } else if (CharSequence.class.isAssignableFrom(argClass)) {

                primCompat = PrimitiveUtils.compatible(Character.class, paramType);

                if (Optional.ofNullable(primCompat).orElse(false)) {
                    int len = arg.toString().length();
                    if (len == 0 || len > 1) mismatchError(argClass, typeName);

                    logger.trace("Parameter is a {}", typeName);
                    javaArgs[i] = arg.toString().charAt(0);

                } else if (paramType.isAssignableFrom(argClass)) {
                    logger.trace("Parameter is a {}", typeName);
                    javaArgs[i] = arg;

                } else mismatchError(argClass, typeName);

            } else {
                //argClass should be NativeArray or NativeObject if the value was not created/derived
                //from a Java call
                String argClassName = argClass.getCanonicalName();
                Type parameterizedType = p.getParameterizedType();
                String ptypeName = parameterizedType.getTypeName();
                
                if (ptypeName.equals(argClassName)) {
                    //This branch will be taken mostly when there is no type information in the parameter
                    //(method signature). For instance: String[], List, MyBean (no parameterized type info). 
                    //Due to type erasure argClassName won't contain any type information. As an example, 
                    //if arg is a List<String>, argClassName will just be like java.util.ArrayList 
                    javaArgs[i] = arg;
                } else {
                    logger.warn("Trying to parse argument of class {} to {}", argClassName, ptypeName);

                    JavaType javaType = mapper.getTypeFactory().constructType(parameterizedType);
                    javaArgs[i] = mapper.convertValue(arguments[i], javaType);
                }
                logger.trace("Parameter is a {}", ptypeName);
            }
        }
        return javaArgs;
        
    }
    
    public GroovyClassLoader getClassLoader() {
        return loader;
    }
    
    private void mismatchError(Class<?> argClass, String typeName) throws IllegalArgumentException {
        throw new IllegalArgumentException(argClass.getSimpleName() + " passed for a " + typeName);
    }
    
    @PostConstruct
    private void init() {

        URL url = null;
        try {
            url = new URL("file://" + econf.getRootDir() + econf.getScriptsPath());
        } catch(MalformedURLException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }
        
        logger.debug("Creating a Groovy Script Engine based at {}", url.toString());        
        gse = new GroovyScriptEngine(new URL[]{ url });
        /*
        //Failed attempt to force scripts have java extension instead of groovy:
        //Dependant scripts are not found if .groovy is not used
        CompilerConfiguration cc = gse.getConfig();
        cc.setDefaultScriptExtension(CLASS_SUFFIX.substring(1));
        
        //Set it so change takes effect
        gse.setConfig(cc);
        */
        loader = gse.getGroovyClassLoader();
        loader.setShouldRecompile(true);

        mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        
    }

}
