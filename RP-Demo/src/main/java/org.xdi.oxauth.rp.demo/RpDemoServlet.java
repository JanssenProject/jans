package org.xdi.oxauth.rp.demo;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author yuriyz on 07/19/2016.
 */
public class RpDemoServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        redirectToLoginIfNeeded(req, resp);
        output(resp);
    }

    private void output(HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html;charset=utf-8");

        PrintWriter pw = resp.getWriter();
        pw.println("<h1>RP Demo</h1>");
    }

    private void redirectToLoginIfNeeded(HttpServletRequest req, HttpServletResponse resp) {
        req.getSession(true).getAttribute("access_token");

    }
}
