package io.jans.agama.engine.servlet;

import io.jans.agama.engine.exception.TemplateProcessingException;
import io.jans.agama.engine.misc.FlowUtils;
import io.jans.agama.engine.page.BasicTemplateModel;
import io.jans.agama.engine.page.Page;
import io.jans.agama.engine.service.TemplatingService;
import io.jans.agama.model.EngineConfig;
import io.jans.util.Pair;

import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.StringWriter;

import org.slf4j.Logger;

public abstract class BaseServlet extends HttpServlet {
    
    @Inject
    protected Logger logger;
    
    @Inject
    protected FlowUtils flowUtils;
    
    @Inject
    private TemplatingService templatingService;
    
    @Inject
    protected EngineConfig engineConf;
    
    @Inject
    protected Page page;

    @Inject
    protected HttpServletRequest request;

    protected boolean isJsonRequest() {
        return MediaType.APPLICATION_JSON.equals(request.getContentType());
    }

    protected void sendNotAvailable(HttpServletResponse response) throws IOException {
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "Flow engine not available. Check if Agama has been enabled in your configuration.");
    }

    protected void sendFlowTimeout(HttpServletResponse response, String message) throws IOException {

        String errorPage = engineConf.getInterruptionErrorPage();
        page.setTemplatePath(errorPath(errorPage));
        page.setDataModel(new BasicTemplateModel(message));
        sendPageContents(response);

    }
    
    protected void sendFlowCrashed(HttpServletResponse response, String error) throws IOException {

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        String errorPage = engineConf.getCrashErrorPage();
        page.setTemplatePath(errorPath(errorPage));
        page.setRawDataModel(new BasicTemplateModel(error));
        sendPageContents(response);
        
    }
    
    protected void sendPageMismatch(HttpServletResponse response, String message, String flowQname)
            throws IOException {

        response.setStatus(HttpServletResponse.SC_NOT_FOUND);

        String errorPage = engineConf.getPageMismatchErrorPage();
        page.setTemplatePath(errorPath(errorPage));
        page.setDataModel(new BasicTemplateModel(message, flowQname));
        sendPageContents(response);

    }

    protected void sendPageContents(HttpServletResponse response) throws IOException {
        
        try {
            processTemplate(response, page.getTemplatePath(), page.getDataModel());
        } catch (TemplateProcessingException e) {

            try {
                response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                String templatePath = errorPath(engineConf.getCrashErrorPage());
                processTemplate(response, templatePath, new BasicTemplateModel(e.getMessage()));

            } catch (TemplateProcessingException e2) {
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, e2.getMessage());
            }
        }

    }
    
    private String errorPath(String page) {
        return isJsonRequest() ? engineConf.getJsonErrorPage(page) : page;
    }
    
    private void processTemplate(HttpServletResponse response, String path, Object dataModel)
            throws TemplateProcessingException, IOException {

        StringWriter sw = new StringWriter();
        Pair<String, String> contentType = templatingService.process(path, dataModel, sw, false);
        
        //encoding MUST be set before calling getWriter 
        response.setCharacterEncoding(contentType.getSecond());
        response.getWriter().write(sw.toString());

        engineConf.getDefaultResponseHeaders().forEach((h, v) -> response.setHeader(h, v));
        String mediaType = contentType.getFirst();
        if (mediaType != null) {
            response.setContentType(mediaType);
        }

    }

}
