package io.jans.casa.core.init;

import io.jans.casa.core.ConfigurationHandler;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Optional;

import static io.jans.casa.misc.AppStateEnum.FAIL;

@WebServlet(urlPatterns = "/health-check")
public class HealthServlet extends HttpServlet {

    @Inject
    private ConfigurationHandler cfgHandler;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (Optional.ofNullable(cfgHandler.getAppState()).map(state -> state.equals(FAIL)).orElse(false)) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } else {
            response.getWriter().print("OK");
            response.setContentType("text/plain");
            response.setDateHeader("Expires", System.currentTimeMillis() + 10000);
        }

    }

}
