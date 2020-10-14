/*
 * Janssen Project software is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.model.net;

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
