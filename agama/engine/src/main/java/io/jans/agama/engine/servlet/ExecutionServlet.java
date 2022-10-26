package io.jans.agama.engine.servlet;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.HttpMethod;
import java.io.IOException;
import java.util.stream.Collectors;

import io.jans.agama.engine.exception.FlowCrashException;
import io.jans.agama.engine.exception.FlowTimeoutException;
import io.jans.agama.engine.model.FlowResult;
import io.jans.agama.engine.model.FlowStatus;
import io.jans.agama.engine.service.FlowService;

@WebServlet(urlPatterns = {
    "*" + ExecutionServlet.URL_SUFFIX,
    ExecutionServlet.CALLBACK_PATH
})
public class ExecutionServlet extends BaseServlet {
    
    public static final String URL_SUFFIX = ".fls"; 
    public static final String URL_PREFIX = "/fl/";
    public static final String CALLBACK_PATH = URL_PREFIX + "callback";
    public static final String ABORT_PARAM = "_abort";
    
    //TODO: put string in agama resource bundle
    private static final String NO_ACTIVE_FLOW = "No flow running currently " +
        "or your flow may have already finished/timed out.";
    
    @Inject
    private FlowService flowService;

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        FlowStatus fstatus = flowService.getRunningFlowStatus();
        if (fstatus == null || fstatus.getStartedAt() == FlowStatus.FINISHED) {
            sendPageMismatch(response, NO_ACTIVE_FLOW, null);
            return;
        }

        String qname = fstatus.getQname();

        if (fstatus.getStartedAt() == FlowStatus.PREPARED) {
            logger.info("Attempting to trigger flow {}", qname);

            try {
                fstatus = flowService.startFlow(fstatus);
                FlowResult result = fstatus.getResult();

                if (result == null) {
                    sendRedirect(response, request.getContextPath(), fstatus, true);
                } else {
                    sendFinishPage(response, result);
                }
            } catch (FlowCrashException e) {
                logger.error(e.getMessage(), e);
                sendFlowCrashed(response, e.getMessage());
            }

        } else {
            String path = request.getServletPath();
            if (processCallback(response, fstatus, path)) return;

            String expectedUrl = getExpectedUrl(fstatus);

            if (path.equals(expectedUrl)) {
                page.setTemplatePath(engineConf.getTemplatesPath() + "/" + fstatus.getTemplatePath());
                page.setDataModel(fstatus.getTemplateDataModel());
                sendPageContents(response);
            } else {
                //This is an attempt to GET a page which is not the current page of this flow
                //json-based clients must explicitly pass the content-type in GET requests
                sendPageMismatch(response, expectedUrl, qname);
            }            
        }
        
    }

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        FlowStatus fstatus = flowService.getRunningFlowStatus();
        if (fstatus == null || fstatus.getStartedAt() == FlowStatus.FINISHED) {
            sendPageMismatch(response, NO_ACTIVE_FLOW, null);
            return;
        }
        
        String path = request.getServletPath();
        if (processCallback(response, fstatus, path)) return;
        
        String expectedUrl = getExpectedUrl(fstatus);
        if (path.equals(expectedUrl)) {
            boolean aborting = request.getParameter(ABORT_PARAM) != null;
            
            String cancelUrl = aborting ? path.substring(URL_PREFIX.length()) : null;
            continueFlow(response, fstatus, false, cancelUrl);
        } else {
            //This is an attempt to POST to a URL which is not the current page of this flow
            sendPageMismatch(response, expectedUrl, fstatus.getQname());
        }
        
    }
    
    @Override
    public void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        
        if (!flowUtils.serviceEnabled()) {
            sendNotAvailable(response);
            return;
        }

        String method = request.getMethod();         
        String path = request.getServletPath();
        boolean match = path.startsWith(URL_PREFIX);

        if (match) {
            logger.debug("ExecutionServlet {} {}", method, path);

            if (method.equals(HttpMethod.GET)) {
                doGet(request, response);
            } else if (method.equals(HttpMethod.POST)) {
                doPost(request, response);
            } else {
                response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            }
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            logger.debug("Unexpected path {}", path);
        }
   
    }
    
    private void continueFlow(HttpServletResponse response, FlowStatus fstatus, boolean callbackResume,
            String cancelUrl) throws IOException {

        try {
            String jsonParams;
            if (isJsonRequest()) {
                //Obtain from payload
                jsonParams = request.getReader().lines().collect(Collectors.joining());
            } else {
                jsonParams = flowUtils.toJsonString(request.getParameterMap());
            }

            fstatus = flowService.continueFlow(fstatus, jsonParams, callbackResume, cancelUrl);
            FlowResult result = fstatus.getResult();

            if (result == null) {
                sendRedirect(response, request.getContextPath(), fstatus,
                        request.getMethod().equals(HttpMethod.GET));
            } else {                    
                sendFinishPage(response, result);
            }
            
        } catch (FlowTimeoutException te) {
            sendFlowTimeout(response, te.getMessage());

        } catch (FlowCrashException ce) {
            logger.error(ce.getMessage(), ce);
            sendFlowCrashed(response, ce.getMessage());
        }

    }
    
    private boolean processCallback(HttpServletResponse response, FlowStatus fstatus, String path)
            throws IOException {

        if (path.equals(CALLBACK_PATH)) {
            if (fstatus.isAllowCallbackResume()) {
                continueFlow(response, fstatus, true, null);
            } else {
                String msg = "Unexpected incoming request to flow callback endpoint";
                logger.warn(msg);
                sendPageMismatch(response, msg, null);
            }
            return true;
        }
        return false;
        
    }
    
    private void sendRedirect(HttpServletResponse response, String contextPath, FlowStatus fls,
            boolean currentIsGet) throws IOException {
        
        String newLocation = fls.getExternalRedirectUrl();
        if (newLocation == null) {
            // Local redirection
            newLocation = contextPath + getExpectedUrl(fls);
        }
        //See https://developer.mozilla.org/en-US/docs/Web/HTTP/Redirections and
        //https://stackoverflow.com/questions/4764297/difference-between-http-redirect-codes
        if (currentIsGet) {
            //This one uses 302 (Found) redirection
            response.sendRedirect(newLocation);
        } else {
            response.setHeader(HttpHeaders.LOCATION, newLocation);
            response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        }
        
    }

    private void sendFinishPage(HttpServletResponse response, FlowResult result) throws IOException {

        String fpage = isJsonRequest() ? engineConf.getJsonFinishedFlowPage() : 
                engineConf.getFinishedFlowPage();
        page.setTemplatePath(fpage);
        page.setDataModel(result);
        sendPageContents(response);
        
    }
    
    private String getExpectedUrl(FlowStatus fls) {
        String templPath = fls.getTemplatePath();
        if (templPath == null) return null;
        return URL_PREFIX + templPath.substring(0, templPath.lastIndexOf(".")) + URL_SUFFIX;
    }
    
}
