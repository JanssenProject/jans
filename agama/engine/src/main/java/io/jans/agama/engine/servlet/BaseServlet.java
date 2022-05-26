package io.jans.agama.engine.servlet;

import io.jans.agama.engine.exception.TemplateProcessingException;
import io.jans.agama.engine.misc.FlowUtils;
import io.jans.agama.engine.page.BasicTemplateModel;
import io.jans.agama.engine.page.Page;
import io.jans.agama.engine.service.TemplatingService;
import io.jans.agama.model.EngineConfig;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;

public abstract class BaseServlet extends HttpServlet {
    
    @Inject
    protected FlowUtils flowUtils;
    
    @Inject
    private TemplatingService templatingService;
    
    @Inject
    protected EngineConfig engineConf;
    
    @Inject
    protected Page page;

    protected boolean isJsonRequest(HttpServletRequest request) {
        return MediaType.APPLICATION_JSON.equals(request.getContentType());
    }
    
    protected void sendNotAvailable(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "Flow engine not available. Check if Agama has been enabled in your configuration.");
    }

    protected void sendFlowTimeout(HttpServletResponse response, boolean jsonResponse, String message)
            throws IOException {

        String errorPage = engineConf.getInterruptionErrorPage();
        page.setTemplatePath(jsonResponse ? engineConf.getJsonErrorPage(errorPage) : errorPage);
        page.setDataModel(new BasicTemplateModel(message));
        sendPageContents(response);

    }
    
    protected void sendFlowCrashed(HttpServletResponse response, boolean jsonResponse, String error)
            throws IOException {

        String errorPage = engineConf.getCrashErrorPage();
        page.setTemplatePath(jsonResponse ? engineConf.getJsonErrorPage(errorPage) : errorPage);
        page.setRawDataModel(new BasicTemplateModel(error));
        sendPageContents(response);
        
    }
    
    protected void sendPageMismatch(HttpServletResponse response, boolean jsonResponse, String url)
            throws IOException {
        
        String errorPage = engineConf.getPageMismatchErrorPage();        
        page.setTemplatePath(jsonResponse ? engineConf.getJsonErrorPage(errorPage) : errorPage);
        page.setDataModel(new BasicTemplateModel(url));
        sendPageContents(response);

    }

    protected void sendPageContents(HttpServletResponse response) throws IOException {
        processTemplate(page.getTemplatePath(), page.getDataModel(), response);        
    }
    
    protected void processTemplate(String path, Object dataModel, HttpServletResponse response)
            throws IOException {

        try {
            engineConf.getDefaultResponseHeaders().forEach((h, v) -> response.setHeader(h, v));
            String mimeType = templatingService.process(path, dataModel, response.getWriter(), false);
            if (mimeType != null) {
                response.setHeader(HttpHeaders.CONTENT_TYPE, mimeType);
            }
        } catch (TemplateProcessingException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e.getMessage());
        }

    }
    
}
