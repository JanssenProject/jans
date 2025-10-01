/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2020, Janssen Project
 */

package io.jans.as.server.servlet;

import io.jans.as.model.configuration.AppConfiguration;
import io.jans.as.model.util.Util;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@WebServlet(urlPatterns = "/firebase-messaging-sw.js")
public class BcFirebaseMessagingSwServlet extends HttpServlet {

    private static final long serialVersionUID = 5445488800130871634L;

    @Inject
    private Logger log;

    @Inject
    private AppConfiguration appConfiguration;

    @Override
    protected void doGet(HttpServletRequest httpServletRequest, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/javascript");
        loadFirebaseMessagingSwFile(response);
    }

    private void loadFirebaseMessagingSwFile(HttpServletResponse response) {
        String baseJavascriptFileConfiguration = "/WEB-INF/firebase-messaging-sw.js";
        try (InputStream in = getServletContext().getResourceAsStream(baseJavascriptFileConfiguration);
             OutputStream out = response.getOutputStream()) {
            String content = IOUtils.toString(in, StandardCharsets.UTF_8);

            Map<String, String> publicConfiguration = new HashMap<>();
            publicConfiguration.put("apiKey", appConfiguration.getCibaEndUserNotificationConfig().getApiKey());
            publicConfiguration.put("authDomain", appConfiguration.getCibaEndUserNotificationConfig().getAuthDomain());
            publicConfiguration.put("databaseURL", appConfiguration.getCibaEndUserNotificationConfig().getDatabaseURL());
            publicConfiguration.put("projectId", appConfiguration.getCibaEndUserNotificationConfig().getProjectId());
            publicConfiguration.put("storageBucket", appConfiguration.getCibaEndUserNotificationConfig().getStorageBucket());
            publicConfiguration.put("messagingSenderId", appConfiguration.getCibaEndUserNotificationConfig().getMessagingSenderId());
            publicConfiguration.put("appId", appConfiguration.getCibaEndUserNotificationConfig().getAppId());

            content = content.replace("'${FIREBASE_CONFIG}'", Util.asJson(publicConfiguration));

            IOUtils.write(content, out, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.debug("Error loading firebase-messaging-sw.js configuration file: " + e.getMessage());
        }
    }

}
