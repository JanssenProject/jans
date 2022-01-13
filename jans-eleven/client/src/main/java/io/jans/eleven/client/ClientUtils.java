/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.eleven.client;

import org.apache.http.client.CookieStore;

/**
 * @author Javier Rojas Blum
 * @version March 20, 2017
 */
public class ClientUtils {

    private ClientUtils() {
    }

    public static void showClient(BaseClient client) {
        System.out.println("-------------------------------------------------------");
        System.out.println("REQUEST:");
        System.out.println("-------------------------------------------------------");
        System.out.println(client.getRequestAsString());
        System.out.println("");

        System.out.println("-------------------------------------------------------");
        System.out.println("RESPONSE:");
        System.out.println("-------------------------------------------------------");
        System.out.println(client.getResponseAsString());
        System.out.println("");
    }

    public static void showClientUserAgent(BaseClient client) {
        System.out.println("-------------------------------------------------------");
        System.out.println("REQUEST:");
        System.out.println("-------------------------------------------------------");
        System.out.println(client.getUrl() + "?" + client.getRequest().getQueryString());
        System.out.println("");

        if (client.getResponse() != null) {
            System.out.println("-------------------------------------------------------");
            System.out.println("RESPONSE:");
            System.out.println("-------------------------------------------------------");
            System.out.println("HTTP/1.1 302 Found");
            System.out.println("Location: " + client.getResponse().getLocation());
            System.out.println("");
        }
    }

    public static void showClient(BaseClient client, CookieStore cookieStore) {
        showClient(client);

        System.out.println("-------------------------------------------------------");
        System.out.println("COOKIES:");
        System.out.println("-------------------------------------------------------");
        System.out.println(cookieStore.getCookies());
        System.out.println("");
    }
}
