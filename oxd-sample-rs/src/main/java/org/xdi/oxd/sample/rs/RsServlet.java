package org.xdi.oxd.sample.rs;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.apache.log4j.Logger;
import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.RegisterSiteParams;
import org.xdi.oxd.common.params.RsProtectParams;
import org.xdi.oxd.common.response.RegisterSiteResponse;
import org.xdi.oxd.common.response.RsProtectResponse;
import org.xdi.oxd.rs.protect.Jackson;
import org.xdi.oxd.rs.protect.RsProtector;
import org.xdi.oxd.rs.protect.RsResource;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Collection;

/**
 * @author Yuriy Zabrovarnyy
 * @version 0.9, 22/06/2016
 */

public class RsServlet extends HttpServlet {

    private static final Logger LOG = Logger.getLogger(RsServlet.class);

    public static final String CONFIGURATION_FILE_NAME = "oxd-sample-rs-config.json";
    public static final String PROTECTION_CONFIGURATION_FILE_NAME = "oxd-sample-rs-protect.json";

    private Configuration configuration;
    private static String oxdId = null;

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        CommandClient client = null;
        try {
            configuration = loadFromJson(inputStream(CONFIGURATION_FILE_NAME));

            Collection<RsResource> values = RsProtector.instance(inputStream(PROTECTION_CONFIGURATION_FILE_NAME)).getResourceMap().values();

            LOG.info("Loaded configuration: " + configuration);
            LOG.info("Resources to protect from configuration: " + values);

            final RegisterSiteResponse site = registerSite(client);

            final RsProtectParams commandParams = new RsProtectParams();
            commandParams.setOxdId(site.getOxdId());
            commandParams.setResources(Lists.newArrayList(values));

            final Command command = new Command(CommandType.RS_PROTECT)
                    .setParamsObject(commandParams);

            final RsProtectResponse resp = client.send(command).dataAsResponse(RsProtectResponse.class);
            Preconditions.checkNotNull(resp);
            Preconditions.checkState(!Strings.isNullOrEmpty(resp.getOxdId()));

            LOG.info("Resource Server started successfully.");
        } catch (Exception e) {
            LOG.error("Failed to start Resource Server. " + e.getMessage(), e);
            throw new ServletException(e);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    public RegisterSiteResponse registerSite(CommandClient client) {

        final RegisterSiteParams commandParams = new RegisterSiteParams();
        commandParams.setOpHost(configuration.getOpHost());
        commandParams.setAuthorizationRedirectUri(configuration.getRedirectUri());
        commandParams.setPostLogoutRedirectUri(configuration.getRedirectUri());
        commandParams.setClientLogoutUri(Lists.newArrayList(configuration.getRedirectUri()));
        commandParams.setScope(Lists.newArrayList("openid", "uma_protection"));

        final Command command = new Command(CommandType.REGISTER_SITE);
        command.setParamsObject(commandParams);

        final RegisterSiteResponse resp = client.send(command).dataAsResponse(RegisterSiteResponse.class);
        Preconditions.checkNotNull(resp);
        Preconditions.checkState(!Strings.isNullOrEmpty(resp.getOxdId()));

        oxdId = resp.getOxdId();
        return resp;
    }

    private InputStream inputStream(String fileName) throws FileNotFoundException {
        ClassLoader classLoader = Configuration.class.getClassLoader();
        File file = new File(ConfigurationLocator.getDir() + fileName);
        if (file.exists()) {
            LOG.trace("Configuration file location: " + ConfigurationLocator.getDir() + fileName);
            return new FileInputStream(file);
        }
        LOG.trace("Loading configuration from class path: " + fileName);
        return classLoader.getResourceAsStream(fileName);
    }

    public static Configuration loadFromJson(InputStream inputStream) {
        try {
            return Jackson.createJsonMapper().readValue(inputStream, Configuration.class);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            return null;
        }
    }

    public static String getOxdId() {
        Preconditions.checkState(!Strings.isNullOrEmpty(oxdId));
        return oxdId;
    }
}
