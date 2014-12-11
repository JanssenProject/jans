/*
 * oxCore is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */package org.xdi.service.sso;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HTTP;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.SimpleLayout;
import org.jboss.seam.Component;
import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.In;
import org.jboss.seam.annotations.Logger;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;
import org.jboss.seam.log.Log;
import org.jboss.seam.log.Logging;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xdi.net.SslDefaultHttpClient;
import org.xdi.service.XmlService;
import org.xdi.util.StringHelper;
import org.xml.sax.SAXException;

/**
 * Action class for headless Shibboleth SSO login
 * 
 * @author Yuriy Movchan Date: 05/29/2013
 */
@Scope(ScopeType.APPLICATION)
@Name("shibbolethLoginService")
@Deprecated
public class ShibbolethLoginService implements Serializable {

	private static final long serialVersionUID = 7409229786722653317L;

	@Logger
	private Log log;
//	private Log log = Logging.getLog(ShibbolethLoginService.class);

	@In
	private XmlService xmlService;

//	static {
//		// Add console appender
//		ConsoleAppender consoleAppender = new ConsoleAppender(new SimpleLayout(), ConsoleAppender.SYSTEM_OUT);
//		LogManager.getRootLogger().removeAllAppenders();
//		LogManager.getRootLogger().addAppender(consoleAppender);
//		org.apache.log4j.Logger.getLogger("org").setLevel(Level.TRACE);
//	}

	private SslDefaultHttpClient httpClient;

	private boolean initialized = false;
	private boolean debug = false;

	public boolean initialize(String certStoreType, String certPath, String certPassword) {
		if (initialized) {
			return true;
		}

		this.httpClient = new SslDefaultHttpClient(certStoreType, certPath, certPassword);

		return true;
	}

	public boolean authenticate(String idpBaseUri, String protectedResourceUri, String userName, String userPassword) {
		if (initialized) {
			return false;
		}

		// Create local context
		HttpContext localContext = new BasicHttpContext();

		// Bind cookie store to the local context
		BasicCookieStore cookieStore = new BasicCookieStore();
		localContext.setAttribute(ClientContext.COOKIE_STORE, cookieStore);

		boolean authenticated = false;
		try {
			authenticated = loginImpl(localContext, idpBaseUri, protectedResourceUri, userName, userPassword);
		} catch (Exception ex) {
			log.error("Exception occured during Shib2 authentication", ex);

			authenticated = false;
		}

		if (!authenticated) {
			return false;
		}

		authenticated = validateCookies(cookieStore);

		if (authenticated) {
			// Log out
		}

		return authenticated;
	}

	/*
	 * public String logout() { // After this redirect we should invalidate this
	 * session try { HttpServletResponse userResponse = (HttpServletResponse)
	 * facesContext.getExternalContext().getResponse(); HttpServletRequest
	 * userRequest = (HttpServletRequest)
	 * facesContext.getExternalContext().getRequest();
	 * 
	 * String redirectUrl = String.format("%s%s",
	 * Configuration.instance().getIdpUrl(), "/idp/logout.jsp"); String url =
	 * String.format("%s://%s/Shibboleth.sso/Logout?return=%s",
	 * userRequest.getScheme(), userRequest.getServerName(), redirectUrl);
	 * 
	 * userResponse.sendRedirect(url); } catch (IOException ex) {
	 * log.error("Failed to redirect to SSO logout page", ex); return false; }
	 * 
	 * return true; }
	 */

	private boolean loginImpl(HttpContext localContext, String idpBaseUri, String protectedResourceUri, String userName, String userPassword)
			throws IOException, ClientProtocolException, SAXException, ParserConfigurationException, XPathExpressionException {
		// Accessing protected resource in order to establish session
		log.debug("Accessing : {0}", protectedResourceUri);

		HttpGet protectedResourceGetMethod = new HttpGet(protectedResourceUri);
		HttpResponse protectedResourceResponse = this.httpClient.execute(protectedResourceGetMethod, localContext);

		// Read response body
		readResponse(localContext, protectedResourceGetMethod, protectedResourceResponse);

		// Attempt to Login into Shib2
		String idpLoginUri = String.format("%s/Authn/UserPassword", idpBaseUri);
		log.debug("Logging in into: {0}", idpLoginUri);

		HttpPost loginPostMethod = new HttpPost(idpLoginUri);
		HttpResponse loginResponse = submitForm(this.httpClient, localContext, loginPostMethod, new String[][] {
				{ "j_username", userName }, { "j_password", userPassword }, });

		// Read response body
		readResponse(localContext, loginPostMethod, loginResponse);

		if (loginResponse.getStatusLine().getStatusCode() != HttpStatus.SC_MOVED_TEMPORARILY) {
			log.warn("Login failed: {0}", userName);
			return false;
		}

		// Go to redirected page
		String location = loginResponse.getFirstHeader("Location").getValue();
		log.debug("Redirecting to location: {0}", location);

		HttpGet locationGetMethod = new HttpGet(location);
		HttpResponse locationResponse = this.httpClient.execute(locationGetMethod, localContext);

		// Read response body
		byte[] locationResponseBytes = readResponse(localContext, locationGetMethod, locationResponse);

		int locationResponseStastusCode = locationResponse.getStatusLine().getStatusCode();
		if (!((locationResponseStastusCode == HttpStatus.SC_MOVED_TEMPORARILY) || (locationResponseStastusCode == HttpStatus.SC_OK))) {
			log.warn("Login failed: {0}", userName);
			return false;
		}

		Document shib2HtmlDoc = xmlService.getXmlDocument(locationResponseBytes);

		boolean shib2HtmlDocValidationResult = validateShib2HtmlDoc(shib2HtmlDoc);
		if (!shib2HtmlDocValidationResult) {
			log.error("Login failed: {0}. The Shib2 form is invalid", userName);
		}

		String shib2SubmitFormUri = getShib2SubmitFormUri(shib2HtmlDoc);
		String[][] shib2SubmitFormParameters = getShib2SubmitFormParameters(shib2HtmlDoc);
		if (StringHelper.isEmpty(shib2SubmitFormUri) || (shib2SubmitFormParameters == null)) {
			log.warn("Login failed: {0}. The Shib2 form is invalid", userName);
			return false;
		}

		HttpPost shib2PostMethod = new HttpPost(shib2SubmitFormUri);
		HttpResponse shib2Response = submitForm(this.httpClient, localContext, shib2PostMethod, shib2SubmitFormParameters);

		// Read response body
		readResponse(localContext, shib2PostMethod, shib2Response);

		int shib2ResponseStastusCode = shib2Response.getStatusLine().getStatusCode();
		if (shib2ResponseStastusCode != HttpStatus.SC_MOVED_TEMPORARILY) {
			log.warn("Login failed: {0}", userName);
			return false;
		}

		// Validate if IDP redirect back to protected resource
		String resultLocation = shib2Response.getFirstHeader("Location").getValue();
		log.debug("Get final redirect to location: {0}", resultLocation);

		boolean result = StringHelper.equalsIgnoreCase(protectedResourceUri, resultLocation);
		log.debug("Authentication result: {0}", result);
		
		return result;
	}

	private HttpResponse submitForm(HttpClient httpClient, HttpContext localContext, HttpRequestBase httpRequest, String[][] parameters)
			throws IOException {
		List<NameValuePair> nvps = new ArrayList<NameValuePair>();

		for (int i = 0; i < parameters.length; i++) {
			nvps.add(new BasicNameValuePair(parameters[i][0], parameters[i][1]));
		}

		((HttpPost) httpRequest).setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));

		return this.httpClient.execute(httpRequest, localContext);
	}

	private byte[] readResponse(HttpContext localContext, HttpRequestBase request, HttpResponse response) throws IOException {
		HttpEntity entity = response.getEntity();
		byte[] responseBytes = new byte[0];
		if (entity != null) {
			responseBytes = EntityUtils.toByteArray(entity);
		}

		if (this.debug) {
			printResponseInfo(localContext, response, responseBytes);
		}

		// Consume response content
		if (entity != null) {
			EntityUtils.consume(entity);
		}

		return responseBytes;
	}

	private boolean validateShib2HtmlDoc(Document shib2HtmlDoc) {
		Node xmlnsNode = shib2HtmlDoc.getFirstChild().getAttributes().getNamedItem("xmlns");
		if (xmlnsNode == null) {
			return false;
		}

		return StringHelper.equalsIgnoreCase("http://www.w3.org/1999/xhtml", xmlnsNode.getNodeValue());
	}

	private String getShib2SubmitFormUri(Document shib2HtmlDoc) throws XPathExpressionException {
		return xmlService.getNodeValue(shib2HtmlDoc, "/html/body/form", "action");
	}

	private String[][] getShib2SubmitFormParameters(Document shib2HtmlDoc) throws XPathExpressionException {
		XPath xPath = XPathFactory.newInstance().newXPath();

		XPathExpression formInputXPathExpression = xPath.compile("/html/body/form/div/input");

		NodeList nodeList = (NodeList) formInputXPathExpression.evaluate(shib2HtmlDoc, XPathConstants.NODESET);
		if (nodeList == null) {
			return null;
		}

		List<String[]> parameters = new ArrayList<String[]>();
		for (int i = 0; i < nodeList.getLength(); i++) {
			Node node = nodeList.item(i);

			Node nodeAttributeName = node.getAttributes().getNamedItem("name");
			Node nodeAttributeValue = node.getAttributes().getNamedItem("value");

			if ((nodeAttributeName == null) || (nodeAttributeValue == null)) {
				continue;
			}

			parameters.add(new String[] { nodeAttributeName.getNodeValue(), nodeAttributeValue.getNodeValue() });
		}

		if (parameters.size() > 0) {
			return parameters.toArray(new String[0][]);
		}

		return null;
	}

	private boolean validateCookies(BasicCookieStore cookieStore) {
		List<org.apache.http.cookie.Cookie> cookies = cookieStore.getCookies();

		boolean foundIdpSession = false;
		boolean foundShibSession = false;
		for (int i = 0; i < cookies.size(); i++) {
			String cookieName = cookies.get(i).getName();
			String cookieValue = cookies.get(i).getValue();

			if (StringHelper.equalsIgnoreCase(cookieName, "_idp_session")) {
				foundIdpSession = StringHelper.isNotEmpty(cookieValue);
				continue;
			}

			if (cookieName.toLowerCase().startsWith("_shibsession")) {
				foundShibSession = StringHelper.isNotEmpty(cookieValue);
				continue;
			}
		}

		return foundIdpSession & foundShibSession;
	}

	public boolean isDebug() {
		return debug;
	}

	public void setDebug(boolean debug) {
		this.debug = debug;
	}

	public void destroy() {
		// When HttpClient instance is no longer needed, shut down the connection manager to ensure immediate deallocation of all system resources
		if (this.httpClient != null) {
			this.httpClient.getConnectionManager().shutdown();
			this.httpClient = null;
		}
	}

	private void printResponseInfo(HttpContext localContext, HttpResponse response, byte[] responseBytes) {
		log.debug("--- START ----------------------------------------");
		printResponseInfo(response, localContext);
		log.debug("--- BODY -----------------------------------------");
		log.debug(new String(responseBytes));
		log.debug("--- END ------------------------------------------");
	}

	private void printResponseInfo(HttpResponse response, HttpContext localContext) {
		log.debug(response.getStatusLine());
		if (response.getEntity() != null) {
			log.debug("Response content length: " + response.getEntity().getContentLength());
		}

		CookieStore cookieStore = (CookieStore) localContext.getAttribute(ClientContext.COOKIE_STORE);
		List<org.apache.http.cookie.Cookie> cookies = cookieStore.getCookies();
		for (int i = 0; i < cookies.size(); i++) {
			log.debug("Local cookie: " + cookies.get(i));
		}
	}

	public static ShibbolethLoginService instance() {
        return (ShibbolethLoginService) Component.getInstance(ShibbolethLoginService.class);
    }

	/*
	public static void main(String[] args) {
		ShibbolethLoginService shibbolethLoginService = new ShibbolethLoginService();
		shibbolethLoginService.initialize("JKS", "D:\\Development\\gluu_conf\\etc\\certs\\shibboleth\\idp.gluu.org.jks", "secret");
		boolean result1 = shibbolethLoginService.authenticate("https://idp.gluu.org/idp", "https://idp.gluu.org/identity/home", "ldap.idp",
				"t5vov7cv");
		boolean result2 = shibbolethLoginService.authenticate("https://idp.gluu.org/idp", "https://idp.gluu.org/identity/home", "ldap.idp",
				"test");
		boolean result3 = shibbolethLoginService.authenticate("https://idp.gluu.org/idp", "https://idp.gluu.org/identity/home", "ldap.idp",
				"t5vov7cv");

		System.out.println("Result1: " + result1);
		System.out.println("Result2: " + result2);
		System.out.println("Result3: " + result3);

	}

	public static class MyRunnable implements Runnable {
		private final ShibbolethLoginService shibbolethLoginService;
		private final int threadIndex;
		private final String userPassword;
		private final boolean expectedResult;
		
		private MyRunnable(ShibbolethLoginService shibbolethLoginService, int threadIndex, String userPassword, boolean expectedResult) {
			this.shibbolethLoginService = shibbolethLoginService;
			this.threadIndex = threadIndex;
			this.userPassword = userPassword;
			this.expectedResult = expectedResult;
		}

		@Override
		public void run() {
			boolean result = shibbolethLoginService.authenticate("https://idp.gluu.org/idp", "https://idp.gluu.org/identity/home",
					"ldap.idp", this.userPassword);

			if (result != this.expectedResult) {
				System.err.println(result);
			}
			System.out.println(threadIndex + " : " + result);
		}
	}

	public static void main(String[] args) {
		ShibbolethLoginService shibbolethLoginService = new ShibbolethLoginService();
		shibbolethLoginService.initialize("JKS", "D:\\Development\\gluu_conf\\etc\\certs\\idp.gluu.org.jks", "secret");

		ExecutorService executor = Executors.newFixedThreadPool(10);
	    for (int i = 0; i < 20; i++) {
	    String userPassword = i % 2 == 0 ? "t5vov7cv" : "wrong_password";
	    System.out.println(i + " : " + userPassword);
	      Runnable worker = new ShibbolethLoginService.MyRunnable(shibbolethLoginService, i, userPassword, i % 2 == 0);
	      executor.execute(worker);
	    }
	}
*/
}
