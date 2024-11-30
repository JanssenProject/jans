package io.jans.agama.engine.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.*;
import java.util.*;

import io.jans.agama.engine.service.*;
import io.jans.jsf2.i18n.ExtendedResourceBundle;
import io.jans.service.cdi.util.CdiUtil;

import org.slf4j.Logger;

//This is not a real Map but pretends to look like one so in Freemarker, templates expressions of the form 
//msgs.KEY or msgs["KEY"] can be used. A HashMap would have led to more concise code but Weld complains
//at startup due to the presence of a final method in it. This leads to proxying issues  
@ApplicationScoped
public class MessagesService extends AbstractMap<String, String> {

    public static final String BUNDLE_ID = "msgs";

    //This returns a null bundle name...
    //factory = (jakarta.faces.application.ApplicationFactory) 
    //    jakarta.faces.FactoryFinder.getFactory(FactoryFinder.APPLICATION_FACTORY);
    //BUNDLE_ID = factory.getApplication().getMessageBundle();
    
    private static final String BUNDLE_BASE_NAME = "jans-auth";

    @Inject
    private Logger logger;
    
    private ExtendedResourceBundle exrBundle;
    
    @Override
    public Set<Map.Entry<String,String>> entrySet() {
        //A dummy implementation suffices
        return Collections.emptySet();
    }

    @Override
    public String get(Object key) {
        
        try {
            //lang is never null if the call originates from an HTTP request
            String lang = CdiUtil.bean(WebContext.class).getLocale().getLanguage();
            //This re-creates a resource bundle instance. However this is cheap because the underlying
            //implementation makes use of Resource#getBundle (by default provides caching)
            //and uses watchers to detect changes in properties files
            exrBundle = new ExtendedResourceBundle(BUNDLE_BASE_NAME, new Locale(lang));
            return exrBundle.getString(key.toString());
        } catch (Exception e) {
            logger.error("Failed to lookup bundle by key '{}': {}", key.toString(), e.getMessage());
            //Prefer "null" over null (null usage causes templates to crash in freemarker)
            return "null";
        }

    }

}
