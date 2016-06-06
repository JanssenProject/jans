/**
 * All rights reserved -- Copyright 2015 Gluu Inc.
 */
package org.xdi.oxd.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandResponse;
import org.xdi.oxd.common.CoreUtils;
import org.xdi.oxd.server.Processor;
import org.xdi.oxd.server.ServerLauncher;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * Created by yuriy on 8/30/2015.
 */
public class CommandServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CommandServlet.class);

    private Processor processor;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        processor = ServerLauncher.getInjector().getInstance(Processor.class);
    }

    private void process(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String commandAsJson = req.getParameter("request");

            LOG.debug("Request, command: " + commandAsJson);

            Command command = new CommandService().validate(commandAsJson);

            String response = CoreUtils.asJson(execute(command));
            LOG.debug("Response, command: " + command + "\n, response: " + response);

            resp.setContentType(MediaType.APPLICATION_JSON);
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.getWriter().println(response);
        } catch (WebApplicationException e) {
            LOG.error(e.getMessage(), e);

            resp.setContentType(MediaType.TEXT_HTML);
            resp.setStatus(e.getResponse().getStatus());
            resp.getWriter().println(e.getResponse().getEntity());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);

            resp.setContentType(MediaType.TEXT_HTML);
            resp.setStatus(500);
            resp.getWriter().println("Internal Server Error: " + e.getMessage());
        }
    }

    public CommandResponse execute(Command command) {
        return processor.process(command);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        process(req, resp);
    }
}
