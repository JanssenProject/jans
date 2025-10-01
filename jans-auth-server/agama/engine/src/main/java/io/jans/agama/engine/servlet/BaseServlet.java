package io.jans.agama.engine.servlet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
import java.util.Map;
import java.io.IOException;
import java.io.StringWriter;

import org.slf4j.Logger;

import static java.nio.charset.StandardCharsets.UTF_8;

public abstract class BaseServlet extends HttpServlet {
    
    private static final String TEMPLATE_PATH_KEY = "_template";
    
    @Inject
    protected Logger logger;
    
    @Inject
    protected FlowUtils flowUtils;
    
    @Inject
    private TemplatingService templatingService;
    
    @Inject
    private ObjectMapper mapper;
    
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

        response.setStatus(HttpServletResponse.SC_GONE);

        String errorPage = engineConf.getInterruptionErrorPage();
        page.setTemplatePath(errorPath(errorPage));
        page.setDataModel(new BasicTemplateModel(message));
        sendPageContents(response);

    }
    
    protected void sendFlowCrashed(HttpServletResponse response, String error) throws IOException {

        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        String errorPage = engineConf.getCrashErrorPage();
        page.setTemplatePath(errorPath(errorPage));
        page.setDataModel(new BasicTemplateModel(error));
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
        sendPageContents(response, false);
    }

    protected void sendPageContents(HttpServletResponse response, boolean nativeClient) throws IOException {
        
        try {
            if (nativeClient) {
                String simplePath = shortenPath(page.getTemplatePath(), 2);
                Object model = page.getAugmentedDataModel(false, Map.of(TEMPLATE_PATH_KEY, simplePath));
                String entity = mapper.writeValueAsString(model);
                processResponse(response, UTF_8.toString(), MediaType.APPLICATION_JSON, entity);                
            } else {
                processTemplate(response, page.getTemplatePath(), page.getAugmentedDataModel(true, null));
            }
        } catch (TemplateProcessingException | JsonProcessingException e) {

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
        processResponse(response, contentType.getSecond(), contentType.getFirst(), sw.toString());        

    }
    
    private void processResponse(HttpServletResponse response, String charset, String mediaType,
            String entity) throws IOException {
    
        //encoding MUST be set before calling getWriter
        response.setCharacterEncoding(charset);        
        engineConf.getDefaultResponseHeaders().forEach((h, v) -> response.setHeader(h, v));
        
        if (mediaType != null) {
            response.setContentType(mediaType);
        }
        response.getWriter().write(entity);

    }

    private String shortenPath(String str, int subPaths) {

        int idx = (str.charAt(0) == '/') ? 1 : 0;
        
        for (int i = 0; i < subPaths; i++) {
            int j = str.indexOf("/", idx);
            if (j == -1) break;
            idx = j + 1;
        }
        return str.substring(idx);
        
    }

}
