package org.xdi.oxauth.servlet;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.jboss.seam.servlet.ContextualHttpServletRequest;
import org.xdi.oxauth.service.SectorIdentifierService;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * @author Javier Rojas Blum
 * @version January 15, 2016
 */
public class SectorIdentifier extends HttpServlet {

    private final static Log LOG = Logging.getLog(OpenIdConfiguration.class);

    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        final HttpServletRequest httpRequest = request;
        final HttpServletResponse httpResponse = response;

        new ContextualHttpServletRequest(httpRequest) {
            @Override
            public void process() throws IOException {
                httpResponse.setContentType("application/json");
                PrintWriter out = httpResponse.getWriter();
                try {
                    String urlPath = httpRequest.getPathInfo();
                    String inum = urlPath.substring(urlPath.lastIndexOf("/") + 1, urlPath.length());

                    org.xdi.oxauth.model.ldap.SectorIdentifier sectorIdentifier = SectorIdentifierService.instance().getSectorIdentifierByInum(inum);

                    JSONArray jsonArray = new JSONArray();

                    for (String redirectUri : sectorIdentifier.getRedirectUris()) {
                        jsonArray.put(redirectUri);
                    }

                    out.println(jsonArray.toString(4).replace("\\/", "/"));
                } catch (JSONException e) {
                    LOG.error(e.getMessage(), e);
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                } finally {
                    out.close();
                }
            }
        }.run();
    }

    /**
     * Handles the HTTP
     * <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Sector Identifier";
    }
}
