/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.service.util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Created by eugeniuparvan on 12/22/16.
 *
 * @author Yuriy Movchan
 */
@ApplicationScoped
public class PageService {
	
	private final String CURRENT_DATE_TIME_FORMATTER = "yyyy-MM-dd hh:mm:ss a";

    public String getRootUrlByRequest(HttpServletRequest request) {
        String url = request.getRequestURL().toString();
        return url.substring(0, url.length() - request.getRequestURI().length());
    }

    public String getCurrentDateTime() {
        DateFormat dateFormat = new SimpleDateFormat(CURRENT_DATE_TIME_FORMATTER);
        return dateFormat.format(new Date());
    }

}
