package io.jans.agama.engine.service;

import groovy.lang.GroovyClassLoader;
import groovy.util.GroovyScriptEngine;
import groovy.util.ResourceException;
import groovy.util.ScriptException;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
    
    @Inject
    private MethodInvoker invoker;
    
    private GroovyScriptEngine gse;
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
        
        //Search for the best matching method/constructor

        if (noInst) {
            if (methodName.equals("new")) return invoker.callConstructor(actionCls, rhinoArgs);

            if (methodName.equals("class")) {
                logger.debug("Returning class object");
                return actionCls;
            }
        }

        return invoker.call(actionCls, instance, methodName, rhinoArgs);

    }
        
    public GroovyClassLoader getClassLoader() {
        return loader;
    }
    
    public Class<?> classFromName(String qname) throws ClassNotFoundException {
        return Class.forName(qname, false, loader);        
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

    }

}
