package io.jans.agama.engine.service;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import groovy.lang.GroovyClassLoader;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.jans.agama.engine.misc.PrimitiveUtils;
import io.jans.agama.model.EngineConfig;

import org.codehaus.groovy.control.CompilerConfiguration;
import org.slf4j.Logger;

@ApplicationScoped
public class ActionService {
    
    private static final List<String> CLASS_EXTENSIONS = Arrays.asList("java", "groovy");
    
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
        Class actionCls = null;
        
        if (!noInst) {
            actionCls = instance.getClass();
            className = actionCls.getName();
        } else {

            try {
                //logger.info("Using current classloader to load class " + className);
                //Try the fastest lookup first
                actionCls = Class.forName(className);
            } catch (ClassNotFoundException e) {

                ResourceException rex = null;
                for (String ext : CLASS_EXTENSIONS) {
                    try {
                        String classFilePath = className.replace('.', File.separatorChar) +  "." + ext;
                        //GroovyScriptEngine classes are only really reloaded when the underlying file changes
                        actionCls = gse.loadScriptByName(classFilePath);
                        break;
                    } catch (ResourceException re) {
                        if (rex == null) rex = re;
                    } catch (ScriptException se) {
                        throw se;
                    } catch (Exception ex) {
                        //Sometimes ScriptException is not thrown when there are parsing problems,
                        //wrapping it here...
                        throw new ScriptException(ex);
                    }
                }
                
                if (actionCls == null) throw new ClassNotFoundException(rex.getMessage(), rex);                
            }
        }
        logger.debug("Class {} loaded successfully", className);
        int arity = rhinoArgs.length;
        
        BiPredicate<Executable, Boolean> pr = (e, staticRequired) -> {
            int mod = e.getModifiers();
            return e.getParameterCount() == arity && Modifier.isPublic(mod) && 
                    (staticRequired ? Modifier.isStatic(mod) : true);
        };
        
        //Search for a method/constructor matching name and arity

        if (noInst) {
            if (methodName.equals("new")) {
                Constructor constr = Stream.of(actionCls.getConstructors()).filter(c -> pr.test(c, false))
                        .findFirst().orElse(null);
                if (constr == null) {
                    String msg = String.format("Unable to find a constructor with arity %d in class %s",
                            arity, className);
                    logger.error(msg);
                    throw new InstantiationException(msg);
                }

                if (logger.isDebugEnabled()) {
                    logger.debug("Constructor found: {}", constr.toGenericString());
                }
                Object[] args = getArgsForCall(constr, arity, rhinoArgs);

                logger.debug("Creating an instance");
                return constr.newInstance(args);

            } else if (methodName.equals("class")) {
                logger.debug("Returning class object");
                return actionCls;
            }
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

        if (logger.isDebugEnabled()) {
            logger.debug("Method found: {}", javaMethod.toGenericString());
        }
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
            
            //argClass does not carry type information due to Java type erasure
            Class<?> argClass = arg.getClass();

            //Try to apply cheaper conversions first (in comparison to mapper-based conversion)
            //Note: A numeric literal coming from Javascript code lands as a Double
            Boolean primCompat = PrimitiveUtils.compatible(argClass, paramType);
            if (primCompat != null) {

                if (primCompat) {
                    logger.trace("Parameter is a primitive (or wrapped) {}", typeName);
                    javaArgs[i] = arg;

                } else if (Number.class.isAssignableFrom(argClass)) {
                    Object number = PrimitiveUtils.primitiveNumberFrom((Number) arg, paramType);
                    
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

                if (straight) {
                    javaArgs[i] = arg;
                } else {
                    logger.warn("Trying to parse argument of class {} to {}", argClass.getCanonicalName(), ptypeName);

                    JavaType javaType = mapper.getTypeFactory().constructType(parameterizedType);
                    javaArgs[i] = mapper.convertValue(arg, javaType);
                }
                logger.trace("Parameter is a {}", ptypeName);
            }
        }
        return javaArgs;
        
    }
    
    public GroovyClassLoader getClassLoader() {
        return loader;
    }
    
    public Class<?> classFromName(String qname) throws ClassNotFoundException {
        return Class.forName(qname, false, loader);        
    }

    private void mismatchError(Class<?> argClass, String typeName) throws IllegalArgumentException {
        throw new IllegalArgumentException(argClass.getSimpleName() + " passed for a " + typeName);
    }
    
    @PostConstruct
    private void init() {

        URL url = null;
        try {
            url = new URL(String.format("file://%s%s/", econf.getRootDir(), econf.getScriptsPath()));
        } catch(MalformedURLException e) {
            logger.error(e.getMessage());
            throw new RuntimeException(e);
        }

        logger.debug("Creating a Groovy Script Engine based at {}", url.toString());        
        gse = new GroovyScriptEngine(new URL[]{ url });

        CompilerConfiguration cc = gse.getConfig();
        cc.setDefaultScriptExtension(CLASS_EXTENSIONS.get(0));
        cc.setScriptExtensions(CLASS_EXTENSIONS.stream().collect(Collectors.toSet()));

        loader = gse.getGroovyClassLoader();
        loader.setShouldRecompile(true);

        mapper = new ObjectMapper();
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    }

}
