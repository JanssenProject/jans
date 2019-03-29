/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.gluu.oxauth.model.error;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * Base class for error responses.
 *
 * @author Javier Rojas Date: 09.22.2011
 *
 */
public abstract class ErrorResponse {

    private final static Logger log = LoggerFactory.getLogger(ErrorResponse.class);

	private String errorDescription;
	private String errorUri;
	private String reason;

	/**
	 * Returns the error code of the response.
	 *
	 * @return The error code.
	 */
	public abstract String getErrorCode();

	/**
	 * If a valid state parameter was present in the request, it returns the
	 * exact value received from the client.
	 *
	 * @return The state value of the request.
	 */
	public abstract String getState();

	/**
	 * Returns a human-readable UTF-8 encoded text providing additional
	 * information, used to assist the client developer in understanding the
	 * error that occurred.
	 *
	 * @return Description about the error.
	 */
	public String getErrorDescription() {
		return errorDescription;
	}

	/**
	 * Sets a human-readable UTF-8 encoded text providing additional
	 * information, used to assist the client developer in understanding the
	 * error that occurred.
	 *
	 * @param errorDescription
	 *            Description about the error.
	 */
	public void setErrorDescription(String errorDescription) {
		this.errorDescription = errorDescription;
	}

	/**
	 * Return an URI identifying a human-readable web page with information
	 * about the error, used to provide the client developer with additional
	 * information about the error.
	 *
	 * @return URI with more information about the error.
	 */
	public String getErrorUri() {
		return errorUri;
	}

	/**
	 * Sets an URI identifying a human-readable web page with information about
	 * the error, used to provide the client developer with additional
	 * information about the error.
	 *
	 * @param errorUri
	 *            URI with more information about the error.
	 */
	public void setErrorUri(String errorUri) {
		this.errorUri = errorUri;
	}

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
	 * Returns a query string representation of the object.
	 *
	 * @return The object represented in a query string.
	 */
	public String toQueryString() {
		StringBuilder queryStringBuilder = new StringBuilder();

		try {
			queryStringBuilder.append("error=").append(getErrorCode());

			if (errorDescription != null && !errorDescription.isEmpty()) {
				queryStringBuilder.append("&error_description=").append(
						URLEncoder.encode(errorDescription, "UTF-8"));
			}

			if (errorUri != null && !errorUri.isEmpty()) {
				queryStringBuilder.append("&error_uri=").append(
						URLEncoder.encode(errorUri, "UTF-8"));
			}

            if (StringUtils.isNotBlank(reason)) {
                queryStringBuilder.append("&reason=").append(URLEncoder.encode(reason, "UTF-8"));
            }

			if (getState() != null && !getState().isEmpty()) {
				queryStringBuilder.append("&state=").append(getState());
			}
		} catch (UnsupportedEncodingException e) {
			log.error(e.getMessage(), e);
			return null;
		}

		return queryStringBuilder.toString();
	}

	/**
	 * Return a JSon string representation of the object.
	 *
	 * @return The object represented in a JSon string.
	 */
	public String toJSonString() {
		JSONObject jsonObj = new JSONObject();

		try {
			jsonObj.put("error", getErrorCode());

			if (errorDescription != null && !errorDescription.isEmpty()) {
				jsonObj.put("error_description", errorDescription);
			}

			if (errorUri != null && !errorUri.isEmpty()) {
				jsonObj.put("error_uri", errorUri);
			}

			if (StringUtils.isNotBlank(reason)) {
			    jsonObj.put("reason", reason);
            }

			if (getState() != null && !getState().isEmpty()) {
				jsonObj.put("state", getState());
			}

		} catch (JSONException e) {
			log.error(e.getMessage(), e);
			return null;
		}

		return jsonObj.toString();
	}
}
