package org.gluu.jsf2.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;

import org.gluu.jsf2.exception.RedirectException;

/**
 * @author Yuriy Movchan
 * @version 03/17/2017
 */
@RequestScoped
@Named
public class FacesService {

	@Inject
	private FacesContext facesContext;

	@Inject
	private ExternalContext externalContext;

	public void redirect(String viewId, Map<String, Object> parameters, boolean includeConversationId) {
		if (viewId == null) {
			throw new RedirectException("cannot redirect to a null viewId");
		}

		String url = facesContext.getApplication().getViewHandler().getRedirectURL(facesContext, viewId,
				Collections.<String, List<String>>emptyMap(), false);

		if (parameters != null) {
			url = encodeParameters(url, parameters);
		}

		redirect(url);
	}

	private void redirect(String url) {
		try {
			externalContext.redirect(externalContext.encodeActionURL(url));
		} catch (IOException ioe) {
			throw new RedirectException(ioe);
		} catch (IllegalStateException ise) {
			throw new RedirectException(ise.getMessage());
		}
	}

	public void redirectToExternalURL(String url) {
		try {
			externalContext.redirect(url);
		} catch (IOException e) {
			throw new RedirectException(e);
		}
	}

	public String encodeParameters(String url, Map<String, Object> parameters) {
		if (parameters.isEmpty()) {
			return url;
		}

		StringBuilder builder = new StringBuilder(url);
		for (Map.Entry<String, Object> param : parameters.entrySet()) {
			String parameterName = param.getKey();
			if (!containsParameter(url, parameterName)) {
				Object parameterValue = param.getValue();
				if (parameterValue instanceof Iterable) {
					for (Object value : (Iterable<?>) parameterValue) {
						builder.append('&').append(parameterName).append('=');
						if (value != null) {
							builder.append(encode(value));
						}
					}
				} else {
					builder.append('&').append(parameterName).append('=');
					if (parameterValue != null) {
						builder.append(encode(parameterValue));
					}
				}
			}
		}

		if (url.indexOf('?') < 0) {
			builder.setCharAt(url.length(), '?');
		}
		return builder.toString();
	}

	private boolean containsParameter(String url, String parameterName) {
		return url.indexOf('?' + parameterName + '=') > 0 || url.indexOf('&' + parameterName + '=') > 0;
	}

	private String encode(Object value) {
		try {
			return URLEncoder.encode(String.valueOf(value), "UTF-8");
		} catch (UnsupportedEncodingException iee) {
			throw new RuntimeException(iee);
		}
	}

}
