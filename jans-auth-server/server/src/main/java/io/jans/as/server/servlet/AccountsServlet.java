package io.jans.as.server.servlet;

import io.jans.as.common.model.registration.Client;
import io.jans.as.model.config.Constants;
import io.jans.as.model.crypto.AbstractCryptoProvider;
import io.jans.as.model.jwk.JSONWebKey;
import io.jans.as.model.jwk.JSONWebKeySet;
import io.jans.as.model.util.CertUtils;
import io.jans.as.server.model.common.AuthorizationGrant;
import io.jans.as.server.service.ClientService;
import io.jans.as.server.service.token.TokenService;
import org.apache.commons.lang.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.util.UUID;

/**
 * @author Javier Rojas Blum
 * @author Yuriy Movchan Date: 2016/04/26
 * @version August 14, 2019
 */

@WebServlet(urlPatterns = "/open-banking/v3.1/aisp/accounts", loadOnStartup = 9)
public class AccountsServlet extends HttpServlet {

    private static final long serialVersionUID = -8224898157373678903L;

    @Inject
    private Logger log;

    @Inject
    private TokenService tokenService;

    @Inject
    private ClientService clientService;

    @Inject
    private AbstractCryptoProvider cryptoProvider;

    @Override
    public void init() throws ServletException {
        log.info("Inside init method of get Accounts Servlet  ***********************************************************************");
    }

    JSONObject getAccount(String nickname, String currency, String accountId, String openingDate, String statusUpdateDateTime, String accountSubType, String status, String accountType) {
        JSONObject account = new JSONObject();
        account.put("Nickname", nickname);
        account.put("Currency", currency);
        account.put("AccountId", accountId);
        account.put("OpeningDate", openingDate);
        account.put("StatusUpdateDateTime", statusUpdateDateTime);
        account.put("AccountSubType", accountSubType);
        account.put("Status", status);
        account.put("AccountType", accountType);
        return account;
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param servletRequest servlet request
     * @param httpResponse   servlet response
     */

    protected void processRequest(HttpServletRequest servletRequest, HttpServletResponse httpResponse) {
        log.info("Starting processRequest method of get Account Servlet***********************************************************************");
        String authFromReq = null;
        String xfapiinteractionid = null;
        String tempaccess_token = null;
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setContentType(Constants.CONTENT_TYPE_APPLICATION_JSON_UTF_8);

        try (PrintWriter out = httpResponse.getWriter()) {

            xfapiinteractionid = servletRequest.getHeader("x-fapi-interaction-id");
            tempaccess_token = servletRequest.getParameter("access_token");

            if (xfapiinteractionid != null) {
                httpResponse.addHeader("x-fapi-interaction-id", xfapiinteractionid);
            } else {
                xfapiinteractionid = UUID.randomUUID().toString();
                httpResponse.addHeader("x-fapi-interaction-id", xfapiinteractionid);
            }

            if ((tempaccess_token != null) && (xfapiinteractionid != null)) {
                if (tempaccess_token.startsWith("Bearer")) {
                    httpResponse.sendError(httpResponse.SC_BAD_REQUEST, "Bearer token in query is disallowed");
                    log.info("FAPI ACcount: Authorization Bearer Token is not allowed in query*********************************************");
                    //throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.ACCESS_DENIED, "FAPI: access_token in query is disallowed.");
                } else {
                    httpResponse.sendError(httpResponse.SC_BAD_REQUEST, "token in query is disallowed");
                    log.info("FAPI: Authorization token is non-Bearer is not allowed in query*********************************************");
                }
            }


            String clientCertAsPem = servletRequest.getHeader("X-ClientCert");
            if (clientCertAsPem != null) {
                log.info("FAPI Account: clientCertAsPem found*****************************************" + clientCertAsPem);
            } else
                log.info("FAPI Account: Nooooooooo clientCertAsPem *****************************************");

            authFromReq = servletRequest.getHeader("Authorization");

            String clientDn = null;
            Client cl = null;
            clientDn = tokenService.getClientDn(authFromReq);
            X509Certificate cert = CertUtils.x509CertificateFromPem(clientCertAsPem);

            AuthorizationGrant authorizationGrant = tokenService.getBearerAuthorizationGrant(authFromReq);

            if (authorizationGrant == null || cert == null) {
                sendError(httpResponse, authorizationGrant == null ? "Unable to find authorization grant." : "Failed to parse client certificate.");
                return;
            }

            PublicKey publicKey = cert.getPublicKey();
            byte[] encodedKey = publicKey.getEncoded();


            if (clientDn != null) {
                log.info("FAPI Account: ClientDn from Authoirization(tokenService) *********************************************" + clientDn);
                cl = clientService.getClientByDn(clientDn);
                JSONObject jsonWebKeys = new JSONObject(cl.getJwks());
                if (jsonWebKeys == null) {
                    log.debug("FAPI Account:********************Unable to load json web keys for client: {}, jwks_uri: {}, jks: {}", cl.getClientId(), cl.getJwksUri(), cl.getJwks());
                }

                int matchctr = 0;
                final JSONWebKeySet keySet = JSONWebKeySet.fromJSONObject(jsonWebKeys);

                try {

                    for (JSONWebKey key : keySet.getKeys()) {
                        if (ArrayUtils.isEquals(encodedKey,
                                cryptoProvider.getPublicKey(key.getKid(), jsonWebKeys, null).getEncoded())) {
                            matchctr += 1;
                            log.debug("FAPI  Account: ********************************Client {} authenticated via `self_signed_tls_client_auth`, matched kid: {}.",
                                    cl.getClientId(), key.getKid());
                        }
                    }

                    if (matchctr == 0) {
                        log.error("FAPI Account: Client certificate does not match clientId. clientId: " + cl.getClientId() + "*********************************************");

                        httpResponse.setStatus(401, "The resource owner or authorization server denied the request");
                        return;
                        //throw new WebApplicationException(Response.status(Response.Status.UNAUTHORIZED).entity(errorResponseFactory.getErrorAsJson(TokenErrorResponseType.INVALID_CLIENT, servletRequest.getParameter("state"), "")).build());
                    }
                } catch (Exception e) {
                    log.info("FAPI Account: Exception while keymatching****************************************************************");
                }
            } else
                log.info("FAPI Account: ClientDn from Authoirization(tokenService) is NULL*********************************************");


            JSONObject jsonObj = new JSONObject();
            JSONArray accounts = new JSONArray();

            jsonObj.put("Links", new JSONObject().put("self", "/open-banking/v3.1/aisp/accounts"));
            jsonObj.put("Meta", new JSONObject().put("TotalPages", 1));
            accounts.put(getAccount("Account1", "GBP", "352413", "05 May 2021", "08 Jun 2021", "CurrentAccount", "Enabled", "Personal"));
            accounts.put(getAccount("Account2", "GBP", "4736325", "25 Mar 2021", "23 Apr 2021", "CurrentAccount", "Enabled", "Personal"));

            jsonObj.put("Data", new JSONObject().put("Account", accounts));

            out.print(jsonObj.toString());
            httpResponse.setStatus(200, "OK");

            out.flush();
            log.info("Finished processRequest method of get Account Servlet ***********************************************************************");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private void sendError(HttpServletResponse response, String error) throws IOException {
        log.error(error);
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, error);
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
