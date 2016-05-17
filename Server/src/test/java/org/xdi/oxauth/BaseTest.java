/*
 * oxAuth is available under the MIT License (2008). See http://opensource.org/licenses/MIT for full text.
 *
 * Copyright (c) 2014, Gluu
 */

package org.xdi.oxauth;

import org.jboss.seam.mock.EnhancedMockHttpServletResponse;
import org.junit.Assert;

/**
 * @author Javier Rojas Date: 10.10.2011
 */
public abstract class BaseTest extends ConfigurableTest {

    public void showTitle(String title) {
        title = "TEST: " + title;

        System.out.println("#######################################################");
        System.out.println(title);
        System.out.println("#######################################################");
    }

    public static void showResponse(String title,
                                    EnhancedMockHttpServletResponse response) {
        System.out.println(" ");
        System.out.println("RESPONSE FOR: " + title);
        System.out.println(response.getStatus());
        for (Object headerName : response.getHeaderNames()) {
            System.out.println(headerName + ": "
                    + response.getHeader(headerName.toString()));
        }
        System.out.println(response.getContentAsString().replace("\\n", "\n"));
        System.out.println(" ");
        System.out.println("Status message: " + response.getStatusMessage());
    }

    public static void fails(Throwable e) {
        Assert.assertNull(e.getMessage(), e);
    }

    public static void output(String p_msg) {
        System.out.println(p_msg);
    }
}