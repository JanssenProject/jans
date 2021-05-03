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

@WebServlet(urlPatterns = "/open-banking/v3.1/aisp/account-access-consents", loadOnStartup = 9)
public class AccontAccessConsentServlet extends HttpServlet {

	private static final long serialVersionUID = -8224898157373678903L;

	@Inject
	private Logger log;

	@Override 
	public void init() throws ServletException
	{
		log.info("Inside init method of AccoutAccess Consent ***********************************************************************");
	}
	
	public static void printJsonObject(JSONObject jsonObj, ServletOutputStream out ) throws IOException {
	    for (String keyStr : jsonObj.keySet()) {
	        Object keyvalue = jsonObj.get(keyStr);
	        //Print key and value
		    out.println("key: "+ keyStr + " value: " + keyvalue);
		    if (keyvalue instanceof JSONObject)
		    	printJsonObject((JSONObject)keyvalue,out);
		}
	}
	
	/**
	 * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
	 * methods.
	 *
	 * @param servletRequest servlet request
	 * @param httpResponse servlet response
	 */
	protected void processRequest(HttpServletRequest servletRequest, HttpServletResponse httpResponse) {
		log.info("Starting processRequest method of AccoutAccess Consent ***********************************************************************");

        try (PrintWriter out = httpResponse.getWriter()) {
        	
        	String jsonBodyStr= IOUtils.toString(servletRequest.getInputStream());
        	JSONObject jsonBody = new JSONObject(jsonBodyStr);

        	httpResponse.setContentType("application/json");
        	String xfapiinteractionid=UUID.randomUUID().toString();;
        	httpResponse.addHeader("x-fapi-interaction-id", xfapiinteractionid);
        	httpResponse.setCharacterEncoding("UTF-8");
        	JSONObject jsonObj = new JSONObject();
        	
        	String permissionKey="";
        	String permissionValue="";
        	
        	for (String keyStr : jsonBody.keySet()) {
    	    	Object keyvalue = jsonBody.get(keyStr);
    		    if (keyStr.equals("Risk"))
        	   	{
        	   		jsonObj.put(keyStr, keyvalue);
        	   	}
    	    	if (keyStr.equals("Data")) {
    	    		JSONObject keyvalueTemp = (JSONObject)jsonBody.get(keyStr);
    		    	for (String keyStr1 : keyvalueTemp.keySet()) {
    		    		Object keyvalue1 = keyvalueTemp.get(keyStr1);
    		    		if (keyStr1.equals("Permissions"))
    		    	   	{
    		    	   		permissionKey=keyStr1; 
    		    	   		permissionValue=keyvalue1.toString();
    		    	   	}
    		    	}
    	    	}
    	    }
    	    	
        	String ConsentId=UUID.randomUUID().toString();
        	jsonObj.put("Links", new JSONObject().put("self","/open-banking/v3.1/aisp/account-access-consents/"+ConsentId));
        	
        	JSONObject data=new JSONObject();
        	Timestamp timestamp = new Timestamp(System.currentTimeMillis());
        	data.put("CreationDateTime", timestamp.getTime());
        	data.put("Status", "AwaitingAuthorisation");
        	data.put(permissionKey, permissionValue);
        	data.put("ConsentId",ConsentId);
        	data.put("StatusUpdateDateTime", timestamp.getTime());
        	jsonObj.put("Data",data);
        	   	
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
