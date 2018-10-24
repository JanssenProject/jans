package org.xdi.oxd.server;

import com.google.inject.Injector;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.xdi.oxd.client.ClientInterface;
import org.xdi.oxd.client.OxdClient;
import org.xdi.oxd.common.params.GetRpParams;
import org.xdi.oxd.common.params.RemoveSiteParams;
import org.xdi.oxd.common.response.GetRpResponse;
import org.xdi.oxd.common.response.RemoveSiteResponse;
import org.xdi.oxd.server.persistence.PersistenceService;
import org.xdi.oxd.server.service.ConfigurationService;
import org.xdi.oxd.server.service.Rp;
import org.xdi.oxd.server.service.RpService;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author yuriyz
 */
public class Cli {

    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        String oxdId = null;
        switchOffLogging();
        try {
            cmd = parser.parse(options(), args);

            oxdId = cmd.getOptionValue("oxd_id");

            Injector injector = ServerLauncher.getInjector();

            injector.getInstance(ConfigurationService.class).setConfiguration(parseConfiguration(cmd.getOptionValue("c")));
            injector.getInstance(PersistenceService.class).create();

            RpService rpService = injector.getInstance(RpService.class);
            rpService.load();

            // list
            if (cmd.hasOption("l")) {
                final Collection<Rp> values = rpService.getRps().values();
                if (values.isEmpty()) {
                    System.out.println("There are no any entries yet in database.");
                    return;
                }

                System.out.println("oxd_id                                client_name");
                for (Rp rp : values) {
                    System.out.println(String.format("%s  %s", rp.getOxdId(), rp.getClientName() != null ? rp.getClientName() : ""));
                }
                return;
            }

            // view by oxd_id
            if (cmd.hasOption("oxd_id")) {
                print(oxdId, rpService.getRp(oxdId));
                return;
            }

            if (cmd.hasOption("d")) {
                // delete
                if (rpService.remove(cmd.getOptionValue("d"))) {
                    System.out.println("Entry removed successfully.");
                } else {
                    System.out.println("Failed to remove entry from database.");
                }
                return;
            }

            System.out.println("Unable to recognize valid parameter.");
            printHelpAndExit();
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            printHelpAndExit();
        } catch (RuntimeException e) {
            // oxd is running and keeps h2 database locked, so we connect to oxd-server and fetch RP via client connection
            if (cmd != null) {
                tryToConnectToRunningOxd(cmd);
            } else {
                printHelpAndExit();
            }
        } catch (Throwable e) {
            System.out.println("Failed to run oxd CLI (make sure oxd-server was run at least one time and database file is created). Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static OxdServerConfiguration parseConfiguration(String pathToYaml) throws IOException, ConfigurationException {
        if (StringUtils.isBlank(pathToYaml)) {
            System.out.println("Path to yml configuration file is not specified. Exit!");
            System.exit(1);
        }
        File file = new File(pathToYaml);
        if (!file.exists()) {
            System.out.println("Failed to find yml configuration file. Please check " + pathToYaml);
            System.exit(1);
        }

        DefaultConfigurationFactoryFactory<OxdServerConfiguration> configurationFactoryFactory = new DefaultConfigurationFactoryFactory<>();
        ConfigurationFactory<OxdServerConfiguration> configurationFactory = configurationFactoryFactory.create(OxdServerConfiguration.class, Validators.newValidatorFactory().getValidator(), Jackson.newObjectMapper(), "dw");
        return configurationFactory.build(file);
    }

    private static void printHelpAndExit() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("utility-name", options());
        System.exit(1);
    }

    private static void switchOffLogging() {
        List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
        loggers.add(LogManager.getRootLogger());
        for (Logger logger : loggers) {
            logger.setLevel(Level.OFF);
        }
    }

    private static int getPort(OxdServerConfiguration conf) {
        final List<ConnectorFactory> applicationConnectors = ((DefaultServerFactory) conf.getServerFactory()).getApplicationConnectors();
        if (applicationConnectors == null || applicationConnectors.isEmpty()) {
            System.out.println("Failed to fetch port from configuration.");
            return -1;
        }
        for (ConnectorFactory connectorFactory : applicationConnectors) {
            if (connectorFactory instanceof HttpConnectorFactory) {
                return ((HttpConnectorFactory) connectorFactory).getPort();
            }
        }
        return -1;
    }

    private static void tryToConnectToRunningOxd(CommandLine cmd) {
        final Injector injector = ServerLauncher.getInjector();
        final OxdServerConfiguration conf = injector.getInstance(ConfigurationService.class).get();
        if (conf == null) {
            System.out.println("Failed to load configuration file of oxd-server.");
            return;
        }

        final int port = getPort(conf);
        if (port == -1) {
            return;
        }

        final ClientInterface client = OxdClient.newClient("https://localhost:" + port);
        String authorization = ""; // todo get authorization here
        try {
            if (cmd.hasOption("l")) {
                GetRpParams params = new GetRpParams();
                params.setList(true);

                GetRpResponse resp = client.getRp(authorization, params);
                if (resp.getNode() instanceof ArrayNode) {
                    final ArrayNode arrayNode = (ArrayNode) resp.getNode();
                    if (arrayNode.size() == 0) {
                        System.out.println("There are no any entries yet in database.");
                        return;
                    }

                    Iterator<JsonNode> elements = arrayNode.getElements();
                    System.out.println("oxd_id                                client_name");
                    while (elements.hasNext()) {
                        final JsonNode element = elements.next();
                        final JsonNode oxdIdNode = element.get("oxd_id");
                        final JsonNode clientNameNode = element.get("client_name");
                        System.out.println(String.format("%s  %s", oxdIdNode != null ? oxdIdNode.asText() : "", clientNameNode != null ? clientNameNode.asText() : "null"));
                    }
                } else {
                    System.out.println(resp.getNode());
                }
                return;
            }

            if (cmd.hasOption("oxd_id")) {
                final String oxdId = cmd.getOptionValue("oxd_id");

                GetRpResponse resp = client.getRp(authorization, new GetRpParams(oxdId));
                if (resp != null) {
                    print(oxdId, resp.getNode());
                } else {
                    System.out.println("Failed to fetch entry from database, please check oxd_id really exist and is not malformed (more details at oxd-server.log file).");
                }
                return;
            }

            if (cmd.hasOption("d")) {
                RemoveSiteResponse resp = client.removeSite(authorization, new RemoveSiteParams(cmd.getOptionValue("d")));
                if (resp != null && StringUtils.isNotBlank(resp.getOxdId())) {
                    System.out.println("Entry removed successfully.");
                } else {
                    System.out.println("Failed to remove entry from database, please check oxd_id really exists and is not malformed (more details in oxd-server.log file).");
                }
                return;
            }
            printHelpAndExit();
        } catch (Exception e) {
            System.out.println("Failed to execute command against oxd-server on port " + port + ", error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String sanitizeOutput(String str) {
        return StringUtils.remove(str, "\"");
    }

    private static void print(String oxdId, Object rp) {
        if (rp != null) {
            System.out.println("JSON for oxd_id " + oxdId);
            System.out.println(rp);
        } else {
            System.out.println("No record found in database for oxd_id: " + oxdId);
        }
    }

    private static Options options() {
        Options options = new Options();

        Option oxdIdOption = new Option("oxd_id", "oxd_id", true, "oxd_id is unique identifier within oxd database (returned by register_site and setup_client commands)");
        oxdIdOption.setRequired(false);
        options.addOption(oxdIdOption);

        Option listOption = new Option("l", "list", false, "lists all oxd_ids contained in oxd database");
        listOption.setRequired(false);
        options.addOption(listOption);

        Option deleteOption = new Option("d", "delete", true, "deletes entry from database by oxd_id");
        deleteOption.setRequired(false);
        options.addOption(deleteOption);

        Option configOption = new Option("c", "config", true, "path to yml configuration file");
        configOption.setRequired(true);
        options.addOption(configOption);

        return options;
    }
}
