package io.jans.agama.engine.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import java.util.Locale;

import io.jans.agama.engine.servlet.RestartServlet;

@RequestScoped
public class WebContext {

    @Inject
    private HttpServletRequest request;
    
    private String contextPath;
    private String relativePath;
    private Locale locale;

    public String getContextPath() {
        return contextPath;
    }
    
    public String getRestartUrl() {
        return contextPath + RestartServlet.PATH;
    }

    public String getRelativePath() {
        return relativePath;
    }
    
    public String getRequestUrl() {
        
        String queryString = request.getQueryString();
        if (queryString == null) {
            queryString = "";
        } else {
            queryString = "?" + queryString;
        }
        return request.getRequestURL().toString() + queryString;

    }
    
    public Locale getLocale() {
        return locale;
    }
    
    @PostConstruct
    private void init() {
        contextPath = request.getContextPath();
        relativePath = request.getServletPath();
        locale = request.getLocale();
    }
    
}
