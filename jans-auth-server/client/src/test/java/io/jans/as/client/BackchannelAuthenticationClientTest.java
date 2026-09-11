/*
 * Janssen Project software is available under the Apache License (2004). See http://www.apache.org/licenses/ for full text.
 *
 * Copyright (c) 2026, Janssen Project
 */

package io.jans.as.client;

import com.sun.net.httpserver.HttpServer;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.WriterAppender;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * @author Yuriy Z
 */
public class BackchannelAuthenticationClientTest {

    private static final String APPENDER_NAME = "BackchannelAuthenticationClientTestAppender";
    private static final String SECRET_PASSWORD = "SuperSecretPwd123";

    private HttpServer httpServer;
    private StringWriter logOutput;
    private WriterAppender appender;
    private LoggerContext loggerContext;

    @BeforeMethod
    public void setUp() {
        logOutput = new StringWriter();

        loggerContext = (LoggerContext) LogManager.getContext(false);
        Configuration config = loggerContext.getConfiguration();

        appender = WriterAppender.createAppender(PatternLayout.newBuilder().withPattern("%m%n").build(),
                null, logOutput, APPENDER_NAME, false, true);
        appender.start();

        LoggerConfig loggerConfig = config.getLoggerConfig(BackchannelAuthenticationClient.class.getName());
        loggerConfig.addAppender(appender, Level.ALL, null);
        loggerContext.updateLoggers();
    }

    @AfterMethod
    public void tearDown() {
        LoggerConfig loggerConfig = loggerContext.getConfiguration().getLoggerConfig(BackchannelAuthenticationClient.class.getName());
        loggerConfig.removeAppender(APPENDER_NAME);
        loggerContext.updateLoggers();
        appender.stop();

        if (httpServer != null) {
            httpServer.stop(0);
        }
    }

    @Test
    public void exec_forMalformedResponseContainingPassword_shouldNotLeakPasswordInLoggedException() throws IOException {
        String responseBody = "not-a-json-response {\"password\":\"" + SECRET_PASSWORD + "\"}";

        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/bc-authorize", exchange -> {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/plain");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        httpServer.start();

        String url = "http://localhost:" + httpServer.getAddress().getPort() + "/bc-authorize";

        BackchannelAuthenticationClient client = new BackchannelAuthenticationClient(url);
        client.setRequest(new BackchannelAuthenticationRequest());

        client.exec();

        String logged = logOutput.toString();
        assertFalse(logged.contains(SECRET_PASSWORD),
                "Logged exception must not contain the raw response body/password: " + logged);
        assertFalse(logged.contains(responseBody),
                "Logged exception must not contain the raw response body: " + logged);
        assertTrue(logged.contains("HTTP 200"),
                "Logged exception should still contain the HTTP status for diagnostics: " + logged);
    }
}
