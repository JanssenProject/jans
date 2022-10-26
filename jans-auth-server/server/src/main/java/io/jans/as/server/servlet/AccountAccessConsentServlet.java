package io.jans.as.server.servlet;

import io.jans.as.common.model.registration.Client;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.token.TokenService;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.UUID;


/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan Date: 2016/04/26
 * @version August 14, 2019
 */

@WebServlet(urlPatterns = "/open-banking/v3.1/aisp/account-access-consents", loadOnStartup = 9)
public class AccountAccessConsentServlet extends HttpServlet {

    private static final long serialVersionUID = -8224898157373678903L;

    @Inject
    private Logger log;

    @Inject
    private TokenService tokenService;

    @Inject
    private ClientService clientService;

    @Override
    public void init() throws ServletException {
        log.info("Inside init method of AccountAccess Consent ***********************************************************************");
    }

    public static void printJsonObject(JSONObject jsonObj, ServletOutputStream out) throws IOException {
        for (String keyStr : jsonObj.keySet()) {
            Object keyvalue = jsonObj.get(keyStr);
            //Print key and value
            out.println("key: " + keyStr + " value: " + keyvalue);
            if (keyvalue instanceof JSONObject)
                printJsonObject((JSONObject) keyvalue, out);
        }
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param servletRequest servlet request
     * @param httpResponse   servlet response
     */
    protected void processRequest(HttpServletRequest servletRequest, HttpServletResponse httpResponse) {
        log.info("Starting processRequest method of AccountAccess Consent ***********************************************************************");
        String authFromReq = null;
        try (PrintWriter out = httpResponse.getWriter()) {

            String jsonBodyStr = IOUtils.toString(servletRequest.getInputStream());
            JSONObject jsonBody = new JSONObject(jsonBodyStr);

            httpResponse.setContentType("application/json");
            String xfapiinteractionid = UUID.randomUUID().toString();
            httpResponse.addHeader("x-fapi-interaction-id", xfapiinteractionid);
            httpResponse.setCharacterEncoding("UTF-8");
            JSONObject jsonObj = new JSONObject();

            String permissionKey = "";
            JSONArray permissionValue = new JSONArray();

            for (String keyStr : jsonBody.keySet()) {
                if (keyStr.equals("data")) {
                    JSONObject keyvalueTemp = (JSONObject) jsonBody.get(keyStr);
                    for (String keyStr1 : keyvalueTemp.keySet()) {
                        Object keyvalue1 = keyvalueTemp.get(keyStr1);
                        if (keyStr1.equals("permissions")) {
                            permissionKey = keyStr1;
                            String tempstr = keyvalue1.toString();
                            String[] temp = tempstr.substring(1, tempstr.length() - 1).split(",");
                            for (int i = 0; i < temp.length; i++)
                                permissionValue.put(temp[i].substring(1, temp[i].length() - 1));
                        }
                        if (keyStr1.equals("expirationDateTime")) {
                            jsonObj.put(keyStr1, keyvalue1.toString());
                        }
                    }
                }
            }

            authFromReq = servletRequest.getHeader("Authorization");

            String clientDn = null;
            Client cl = null;
            String clientID = null;
            String ConsentID = null;
            clientDn = tokenService.getClientDn(authFromReq);

            if (clientDn != null) {
                log.info("FAPIOBUK: ClientDn from Authoirization(tokenService) *********************************************" + clientDn);
                cl = clientService.getClientByDn(clientDn);
                clientID = cl.getClientId();
            } else
                log.info("FAPIOBUK: ClientDn is null");

            if (clientID != null)
                ConsentID = UUID.randomUUID().toString() + ":" + clientID;
            else {
                ConsentID = UUID.randomUUID().toString();
                log.info("FAPIOBUK: ClientID is null");
            }
            jsonObj.put("links", new JSONObject().put("self", "/open-banking/v3.1/aisp/account-access-consents/" + ConsentID));

            JSONObject data = new JSONObject();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            data.put("creationDateTime", timestamp.getTime());
            data.put("status", "AwaitingAuthorisation");
            data.put(permissionKey, permissionValue);
            data.put("consentId", ConsentID);
            data.put("statusUpdateDateTime", timestamp.getTime());
            jsonObj.put("data", data);

            out.print(jsonObj.toString());
            httpResponse.setStatus(201, "Created");

            out.flush();
            log.info("Finished processRequest method of AccoutAccess Consent ***********************************************************************");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }


    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Account Access Consent";
    }
}
