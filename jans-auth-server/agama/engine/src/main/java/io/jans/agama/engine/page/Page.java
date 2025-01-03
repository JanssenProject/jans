package io.jans.agama.engine.page;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import java.util.*;

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

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public Object getDataModel() {
        return dataModel;
    }
    
    public Object getAugmentedDataModel(boolean includeContextualData, Map<String, Object> extra) {
        
        Map<String, Object> model = new HashMap<>(dataModel);
        
        if (includeContextualData) {
            model.putIfAbsent(WEB_CTX_KEY, webContext);
            model.putIfAbsent(MessagesService.BUNDLE_ID, msgsService);
            model.putIfAbsent(LabelsService.METHOD_NAME, labelsService);
            model.putIfAbsent(CACHE_KEY, cache);
        }
        if (extra != null) {
            extra.forEach((k, v) -> model.putIfAbsent(k, v));
        }        
        return model;
        
    }

    public void setDataModel(Object object) {
        dataModel = object == null ? Map.of() : mapFromObject(object);
    }
    
    private Map<String, Object> mapFromObject(Object object) {
        return mapper.convertValue(object, new TypeReference<Map<String, Object>>(){});
    }

    @PostConstruct
    private void init() {
        dataModel = Map.of();
    }
    
}
