package io.jans.agama.engine.servlet;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;
import java.io.IOException;

import io.jans.agama.NativeJansFlowBridge;
import io.jans.agama.engine.exception.FlowTimeoutException;
import io.jans.agama.engine.model.FlowStatus;
import io.jans.agama.engine.service.FlowService;

@WebServlet(urlPatterns = RestartServlet.PATH)
public class RestartServlet extends BaseServlet {

    public static final String PATH = "/fl/restart";
    
    @Inject
    private FlowService flowService;

    @Inject
    private NativeJansFlowBridge bridge;

    @Override
    public void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        if (!flowUtils.serviceEnabled()) {
            sendNotAvailable(response);
            return;
        }

        logger.debug("Restart servlet");
        try {
            FlowStatus st = flowService.getRunningFlowStatus();

            if (st == null || st.getStartedAt() == FlowStatus.FINISHED) {
                //If flow exists, ensure it is not about to be collected by cleaner job
                throw new IOException("No flow to restart");
            } else {
                
                try {
                    flowService.ensureTimeNotExceeded(st);
                    flowService.terminateFlow();

                    logger.debug("Sending user's browser for a flow start");
                    //This redirection relies on the (unauthenticated) session id being still alive 
                    //(so the flow name and its inputs can be remembered in the bridge script)
                    //see AgamaBridge.py#prepareForStep
                    String url = bridge.scriptPageUrl().replaceFirst("\\.xhtml", ".htm");
                    response.sendRedirect(request.getContextPath() + "/" + url);

                } catch (FlowTimeoutException e) {
                    sendFlowTimeout(response, e.getMessage());
                }
                
            }
        } catch (IOException e) {
            sendFlowCrashed(response, e.getMessage());
        }

    }

}
