package org.xdi.service.util;

import org.jboss.seam.ScopeType;
import org.jboss.seam.annotations.AutoCreate;
import org.jboss.seam.annotations.Name;
import org.jboss.seam.annotations.Scope;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by eugeniuparvan on 12/22/16.
 * 
 * @author Yuriy Movchan
 */
@Name("pageService")
@Scope(ScopeType.STATELESS)
@AutoCreate
public class PageService {

	private static final DateFormat currentDateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss a");

	public String getRootUrlByRequest(HttpServletRequest request) {
		String url = request.getRequestURL().toString();
		return url.substring(0, url.length() - request.getRequestURI().length());
	}

	public String getCurrentDateTime() {
		return currentDateTimeFormatter.format(new Date());
	}

}
