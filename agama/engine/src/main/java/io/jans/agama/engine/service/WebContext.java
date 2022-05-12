package io.jans.agama.engine.service;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;

import io.jans.agama.engine.servlet.RestartServlet;

@RequestScoped
public class WebContext {

    @Inject
    private HttpServletRequest request;
    
    private String contextPath;
    private String relativePath;
    private String rpFlowInitiatorUrl;

    public String getContextPath() {
        return contextPath;
    }
    
    public String getRestartUrl() {
        return contextPath + RestartServlet.PATH;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getRpFlowInitiatorUrl() {
        return rpFlowInitiatorUrl;
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

    public void setRpFlowInitiatorUrl(String rpFlowInitiatorUrl) {
        this.rpFlowInitiatorUrl = rpFlowInitiatorUrl;
    }
    
    @PostConstruct
    private void init() {
        contextPath = request.getContextPath();
        relativePath = request.getServletPath();
    }
    
}
