package io.jans.agama.engine.page;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import java.util.HashMap;
import java.util.Map;

import io.jans.agama.engine.service.*;
import io.jans.service.CacheService;

@RequestScoped
public class Page {

    private static final String WEB_CTX_KEY = "webCtx";
    private static final String CACHE_KEY = "cache";
    
    @Inject
    private WebContext webContext;
    
    @Inject
    private ObjectMapper mapper;
    
    @Inject
    private MessagesService msgsService;

    @Inject
    private LabelsService labelsService;

    @Inject
    private CacheService cache;
    
    private String templatePath;
    private Map<String, Object> dataModel;
    private Object rawModel;

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public Object getDataModel() {
        
        if (rawModel == null) {
            if (dataModel != null) {

                dataModel.putIfAbsent(WEB_CTX_KEY, webContext);
                dataModel.putIfAbsent(MessagesService.BUNDLE_ID, msgsService);
                dataModel.putIfAbsent(LabelsService.METHOD_NAME, labelsService);
                dataModel.putIfAbsent(CACHE_KEY, cache);
                return dataModel;

            } else return new Object();
        } else return rawModel;
        
    }

    /**
     * This call is cheaper than setDataModel, but pages won't have access to any
     * contextual data
     * @param object 
     */
    public void setRawDataModel(Object object) {       
        rawModel = object;
        dataModel = null;
    }

    public void setDataModel(Object object) {
        rawModel = null;
        dataModel = mapFromObject(object);
    }
    
    public void appendToDataModel(Object object) {
        if (rawModel != null) {
            rawModel = null;
            dataModel = new HashMap<>();
        }
        dataModel.putAll(mapFromObject(object));
    }
    
    private Map<String, Object> mapFromObject(Object object) {
        return mapper.convertValue(object, new TypeReference<Map<String, Object>>(){});
    }

    @PostConstruct
    private void init() {
        dataModel = new HashMap<>();
    }
    
}
