/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.model.error;

import io.jans.as.model.config.Constants;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Base class for error responses.
 *
 * @author Javier Rojas Blum
 * @version August 20, 2019
 */
public class ErrorResponse {

    private static final Logger log = Logger.getLogger(ErrorResponse.class);

	private int status;
	private String errorCode;
	private String errorDescription;
	private String errorUri;
	private String reason;
	private String state;

	/**
	 * Return the HTTP response status code.
	 *
	 * @return The response status.
	 */
	public int getStatus() {
		return status;
	}

	/**
	 * Sets the HTTP response status code.
	 *
	 * @param status The response status.
	 */
	public void setStatus(int status) {
		this.status = status;
	}

	/**
	 * If a valid state parameter was present in the request, it returns the
	 * exact value received from the client.
	 *
	 * @return The state value of the request.
	 */
	public String getState() {
	    return state;
    }

    public void setState(String state) {
        this.state = state;
    }

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
     * Returns the error code of the response.
     *
     * @return The error code.
     */
    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
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
						URLEncoder.encode(errorDescription, StandardCharsets.UTF_8.name()));
			}

			if (errorUri != null && !errorUri.isEmpty()) {
				queryStringBuilder.append("&error_uri=").append(
						URLEncoder.encode(errorUri, StandardCharsets.UTF_8.name()));
			}

            if (StringUtils.isNotBlank(reason)) {
                queryStringBuilder.append("&reason=").append(URLEncoder.encode(reason, StandardCharsets.UTF_8.name()));
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
				jsonObj.put(Constants.ERROR_URI, errorUri);
			}

			if (StringUtils.isNotBlank(reason)) {
			    jsonObj.put(Constants.REASON, reason);
            }

			if (getState() != null && !getState().isEmpty()) {
				jsonObj.put(Constants.STATE, getState());
			}

			return jsonObj.toString(4).replace("\\/", "/");
		} catch (JSONException e) {
			log.error(e.getMessage(), e);
			return null;
		}
	}
}
