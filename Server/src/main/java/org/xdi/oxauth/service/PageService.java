package org.xdi.oxauth.service;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by eugeniuparvan on 12/22/16.
 */
@Name("pageService")
@Scope(ScopeType.STATELESS)
public class PageService {
    public String getRootUrlByRequest(HttpServletRequest request){
        String url = request.getRequestURL().toString();
        return url.substring(0, url.length() - request.getRequestURI().length());
    }
}
