/**
 * 
 */
package org.gluu.oxauth.model.net;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpRequestBase;

import java.io.Serializable;

/**
 * @author Yuriy Movchan Date: 07/14/2015
 *
 */
public class HttpServiceResponse implements Serializable {

	private static final long serialVersionUID = 2218884738060554709L;

	private HttpRequestBase httpRequest;
	private HttpResponse httpResponse;

	public HttpServiceResponse(HttpRequestBase httpRequest, HttpResponse httpResponse) {
		this.httpRequest = httpRequest;
		this.httpResponse = httpResponse;
	}

	public HttpRequestBase getHttpRequest() {
		return httpRequest;
	}

	public HttpResponse getHttpResponse() {
		return httpResponse;
	}

	public void closeConnection() {
		if (httpRequest == null) {
			return;
		}
		
		httpRequest.releaseConnection();
	}

}
