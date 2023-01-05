/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.jsf2.service;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jakarta.enterprise.context.RequestScoped;
import jakarta.faces.application.NavigationHandler;
import jakarta.faces.application.ViewHandler;
import jakarta.faces.component.UIViewRoot;
import jakarta.faces.context.ExternalContext;
import jakarta.faces.context.FacesContext;
import jakarta.faces.context.PartialViewContext;
import jakarta.inject.Inject;
import jakarta.inject.Named;

import io.jans.jsf2.exception.RedirectException;

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

    public void redirect(String viewId) {
        redirect(viewId, null);
    }

    public void redirectWithExternal(String redirectTo, Map<String, Object> parameters) {
        if (redirectTo.startsWith("https") || redirectTo.startsWith("http")) {
            redirectToExternalURL(redirectTo);
        } else {
            redirect(redirectTo, parameters);
        }
    }

    public String resolveUrl(String viewId) {
        if (viewId == null) {
            throw new RedirectException("Cannot redirect to a null viewId");
        }

        String url = facesContext.getApplication().getViewHandler().getRedirectURL(facesContext, viewId, Collections.<String, List<String>>emptyMap(), false);
        
        return url;
    }

    public void redirect(String viewId, Map<String, Object> parameters) {
        if (viewId == null) {
            throw new RedirectException("cannot redirect to a null viewId");
        }

        String url = facesContext.getApplication().getViewHandler().getRedirectURL(facesContext, viewId,
                Collections.<String, List<String>>emptyMap(), false);

        if (parameters != null) {
            url = encodeParameters(url, parameters);
        }

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

    public void renderView(String viewId) {
        final FacesContext ctx = FacesContext.getCurrentInstance();
        final ViewHandler viewHandler = ctx.getApplication().getViewHandler();

        UIViewRoot newRoot = viewHandler.createView(ctx, viewId); 
        updateRenderTargets(ctx, viewId);
        ctx.setViewRoot(newRoot);
        clearViewMapIfNecessary(ctx, viewId);

        ctx.renderResponse();
    }
    
    private void updateRenderTargets(FacesContext ctx, String newId) {
        if (ctx.getViewRoot() == null || !ctx.getViewRoot().getViewId().equals(newId)) {
            PartialViewContext pctx = ctx.getPartialViewContext();
            if (!pctx.isRenderAll()) {
                pctx.setRenderAll(true);
            }
        }
    }
    private void clearViewMapIfNecessary(FacesContext facesContext, String newId) {
        UIViewRoot root = facesContext.getViewRoot();

        if (root != null && !root.getViewId().equals(newId)) {
            Map<String, Object> viewMap = root.getViewMap(false);
            if (viewMap != null) {
                viewMap.clear();
            }
        }
    }

    public void navigateToView(String fromAction, String outcome, Map<String, Object> parameters) {
        final FacesContext fc = FacesContext.getCurrentInstance();

        Map<String, Object> requestMap = fc.getExternalContext().getRequestMap();
        NavigationHandler nav = fc.getApplication().getNavigationHandler();

        if (parameters != null) {
            requestMap.putAll(parameters);
        }
        nav.handleNavigation(fc, fromAction, outcome);
        fc.renderResponse();
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
