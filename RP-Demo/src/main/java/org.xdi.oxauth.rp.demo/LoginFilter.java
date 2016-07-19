package org.xdi.oxauth.rp.demo;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author yuriyz on 07/19/2016.
 */
public class LoginFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        Object accessToken = request.getSession(true).getAttribute("access_token");
        if (accessToken == null) {
            redirectToLogin(servletRequest, servletResponse);
        } else {
            filterChain.doFilter(servletRequest, servletResponse);
        }
    }

    private void redirectToLogin(ServletRequest servletRequest, ServletResponse servletResponse) {

    }

    @Override
    public void destroy() {

    }
}
