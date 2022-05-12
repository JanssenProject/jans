package io.jans.agama.engine.page;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.faces.FactoryFinder;
import jakarta.faces.application.Application;
import jakarta.faces.application.ApplicationFactory;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ResourceBundle;
import java.util.Set;

import org.slf4j.Logger;

//This is not a real Map but pretends to look like one so in Freemarker templates expressions of 
//the form msgs.KEY can be used. A HashMap would have led to more concise code but Weld complains
//at startup due to the presence of a final method in it. This leads to proxying issues  
@ApplicationScoped
public class Labels extends AbstractMap<String, String> {
    
    public static final String BUNDLE_ID = "msgs";

    @Inject
    private Logger logger;
    
    private Application facesApp;
    
    @Override
    public Set<Map.Entry<String,String>> entrySet() {
        return Collections.emptySet();
    }
    
    @Override
    public String get(Object key) {
        
        try {
            /*FacesContext ctx = FacesContext.getCurrentInstance();
            ResourceBundle bundle = ctx.getApplication().getResourceBundle(ctx, BUNDLE_ID);*/
            ResourceBundle bundle = facesApp.getResourceBundle(FacesContext.getCurrentInstance(), BUNDLE_ID);
            return bundle.getString(key.toString());
        } catch (Exception e) {
            logger.error("Failed to lookup resource bundle by key '{}': {}", key.toString(), e.getMessage());
            return null;
        }
        
    }
    
    @PostConstruct
    private void init() {
        ApplicationFactory factory = (ApplicationFactory) FactoryFinder.getFactory(FactoryFinder.APPLICATION_FACTORY);
        facesApp = factory.getApplication();        
    }
    
}
