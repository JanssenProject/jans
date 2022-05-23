/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.ca.server.arquillian;

import io.jans.ca.client.ClientInterface;
import io.jans.ca.server.Tester;
import jakarta.ws.rs.core.Response;
import org.testng.Assert;

import java.net.URI;
import java.util.List;
import java.util.Map;

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

    public static void showTitle(String title, String targetHostApi) {
        title = "TEST: " + title;

        System.out.println("#######################################################");
        System.out.println(title);
        System.out.println("Target host Client Api: " + targetHostApi);
        System.out.println("#######################################################");
    }

    public static void fails(Throwable e) {
        Assert.fail(e.getMessage(), e);
    }

    public static void output(String msg) {
        System.out.println(msg);
    }

    public void showResponse(String title, Response response) {
        showResponse(title, response, null);
    }

    public static void showResponse(String title, Response response, Object entity) {
        System.out.println(" ");
        System.out.println("RESPONSE FOR: " + title);
        System.out.println(response.getStatus());
        for (Map.Entry<String, List<Object>> headers : response.getHeaders().entrySet()) {
            String headerName = headers.getKey();
            System.out.println(headerName + ": " + headers.getValue());
        }

        if (entity != null) {
            System.out.println(entity.toString().replace("\\n", "\n"));
        }
        System.out.println(" ");
        System.out.println("Status message: " + response.getStatus());
    }

    public static String getApiTagetURL(URI uriArquillianTestServer) {
        if (uriArquillianTestServer != null) {
            return uriArquillianTestServer.toString();
        } else {
            return System.getProperty("test.client.api.url");
        }
    }

    public static ClientInterface getClientInterface(URI uriArquillianTestServer) {
        String urlEndPoint = getApiTagetURL(uriArquillianTestServer);
        return Tester.newClient(urlEndPoint);
    }

}