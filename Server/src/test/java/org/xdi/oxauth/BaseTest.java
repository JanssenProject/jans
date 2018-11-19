/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth;

import org.testng.Assert;

import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map.Entry;

/**
 * @author Javier Rojas
 * @author Yuriy Movchan Date: 10.10.2011
 */
public abstract class BaseTest extends ConfigurableTest {

	public static void showTitle(String title) {
		title = "TEST: " + title;

		System.out.println("#######################################################");
		System.out.println(title);
		System.out.println("#######################################################");
	}

	public void showResponse(String title, Response response) {
		showResponse(title, response, null);
	}

	public static void showResponse(String title, Response response, Object entity) {
		System.out.println(" ");
		System.out.println("RESPONSE FOR: " + title);
		System.out.println(response.getStatus());
		for (Entry<String, List<Object>> headers : response.getHeaders().entrySet()) {
			String headerName = headers.getKey();
			System.out.println(headerName + ": " + headers.getValue());
		}

		if (entity != null) {
			System.out.println(entity.toString().replace("\\n", "\n"));
		}
		System.out.println(" ");
		System.out.println("Status message: " + response.getStatus());
	}

	public static void fails(Throwable e) {
		Assert.fail(e.getMessage(), e);
	}

	public static void output(String p_msg) {
		System.out.println(p_msg);
	}
}