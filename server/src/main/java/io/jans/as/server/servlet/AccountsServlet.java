package io.jans.as.server.servlet;

import javax.servlet.http.HttpServlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;

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

	@Override 
	public void init() throws ServletException
	{
		log.info("Inside init method of get Accounts Servlet  ***********************************************************************");
	}
	
	JSONObject  getAccount(String nickname, String currency, String accountId, String openingDate, String statusUpdateDateTime, String accountSubType, String status, String accountType){
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
	 * @param httpResponse servlet response
	 */

    protected void processRequest(HttpServletRequest servletRequest, HttpServletResponse httpResponse) {
		log.info("Starting processRequest method of get Account Servlet***********************************************************************");
    	String xfapiinteractionid = null;
    	String tempaccess_token = null;
		try (PrintWriter out = httpResponse.getWriter()) {
        	
        	httpResponse.setContentType("application/json");
        	    	
        	xfapiinteractionid = servletRequest.getHeader("x-fapi-interaction-id");
        	tempaccess_token=servletRequest.getParameter("access_token"); 
        	
        	if (xfapiinteractionid!=null) {
             	httpResponse.addHeader("x-fapi-interaction-id", xfapiinteractionid);
            }
            else { 
            	xfapiinteractionid=UUID.randomUUID().toString();;
             	httpResponse.addHeader("x-fapi-interaction-id", xfapiinteractionid);
            }
        	
        	if ((tempaccess_token !=null) && (xfapiinteractionid != null) ) {
	            if (tempaccess_token.startsWith("Bearer")) {
	            	httpResponse.sendError(httpResponse.SC_BAD_REQUEST, "Bearer token in query is disallowed"); 
	            	log.info("FAPI: Authorization Bearer Token is not allowed in query*********************************************");
	                //throw errorResponseFactory.createWebApplicationException(Response.Status.BAD_REQUEST, RegisterErrorResponseType.ACCESS_DENIED, "FAPI: access_token in query is disallowed.");
	            }
	            else {
	            	httpResponse.sendError(httpResponse.SC_BAD_REQUEST, "token in query is disallowed");
	            	log.info("FAPI: Authorization token is non-Bearer is not allowed in query*********************************************");
	            }
        	}       	
               	
        	httpResponse.setCharacterEncoding("UTF-8");
        	JSONObject jsonObj = new JSONObject();
        	JSONArray accounts= new JSONArray();
        	
        	jsonObj.put("Links", new JSONObject().put("self","/open-banking/v3.1/aisp/accounts"));
        	jsonObj.put("Meta", new JSONObject().put("TotalPages",1));
        	accounts.put(getAccount("Account1","GBP", "352413", "05 May 2021", "08 Jun2021", "CurrentAccount", "Enabled","Personal"));
        	accounts.put(getAccount("Account1","GBP", "4736325", "25 Mar 2021", "23 Apr 2021", "CurrentAccount", "Enabled","Personal"));
        	
 		   	jsonObj.put("Data", new JSONObject().put("Account",accounts));
        	
 		   	out.print(jsonObj.toString());
        	httpResponse.setStatus(200, "OK");

        	out.flush();
        	log.info("Finished processRequest method of get Account Servlet ***********************************************************************");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
	}
    
	/**
	 * Handles the HTTP <code>GET</code> method.
	 *
	 * @param request servlet request
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
	 * @param request servlet request
	 * @param response servlet response
	 * @throws IOException if an I/O error occurs
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)	throws IOException {
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
