package io.jans.as.server.servlet;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.token.TokenService;
import jakarta.inject.Inject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

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

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    public void init() {
        log.info("AccountAccessConsentServlet initialized.");
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param servletRequest servlet request
     * @param httpResponse   servlet response
     */
    protected void processRequest(HttpServletRequest servletRequest, HttpServletResponse httpResponse) {
        log.debug("AccountAccessConsentServlet - Starting processRequest ...");

        String authFromReq = null;
        try (PrintWriter out = httpResponse.getWriter()) {

            String jsonBodyStr = IOUtils.toString(servletRequest.getInputStream());
            JSONObject jsonBody = new JSONObject(jsonBodyStr);

            httpResponse.setContentType("application/json");
            httpResponse.addHeader("x-fapi-interaction-id", UUID.randomUUID().toString());
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
                            for (String s : temp) {
                                permissionValue.put(s.substring(1, s.length() - 1));
                            }
                        }
                        if (keyStr1.equals("expirationDateTime")) {
                            jsonObj.put(keyStr1, keyvalue1.toString());
                        }
                    }
                }
            }

            authFromReq = servletRequest.getHeader("Authorization");

            String clientDn = null;
            Client client = null;
            String clientID = null;
            String consentID = null;
            clientDn = tokenService.getClientDn(authFromReq);

            if (StringUtils.isNotBlank(clientDn)) {
                client = clientService.getClientByDn(clientDn);
                clientID = client.getClientId();
            }

            log.debug("AccountAccessConsentServlet - processRequest, clientDn: {}", clientDn);

            if (clientID != null)
                consentID = UUID.randomUUID().toString() + ":" + clientID;
            else {
                consentID = UUID.randomUUID().toString();
                log.info("FAPIOBUK: ClientID is null");
            }
            jsonObj.put(capitalize("links"), new JSONObject().put(capitalize("self"), "/open-banking/v3.1/aisp/account-access-consents/" + consentID));

            JSONObject data = new JSONObject();
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            data.put(capitalize("creationDateTime"), timestamp.getTime());
            data.put(capitalize("status"), "AwaitingAuthorisation");
            data.put(permissionKey, permissionValue);
            data.put(capitalize("consentId"), consentID);
            data.put(capitalize("statusUpdateDateTime"), timestamp.getTime());
            jsonObj.put(capitalize("data"), data);

            out.print(jsonObj.toString());
            httpResponse.setStatus(201, "Created");

            out.flush();
            log.debug("AccountAccessConsentServlet - Finished processRequest.");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private String capitalize(String key) {
        final boolean uppercase = BooleanUtils.isTrue(appConfiguration.getUppercaseResponseKeysInAccountAccessConsent());
        return uppercase ? StringUtils.capitalize(key) : key;
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
        return "AccountAccessConsentServlet";
    }
}
