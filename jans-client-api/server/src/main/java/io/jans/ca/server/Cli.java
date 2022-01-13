package io.jans.ca.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.google.inject.Injector;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import io.dropwizard.jetty.HttpsConnectorFactory;
import io.dropwizard.server.DefaultServerFactory;
import io.jans.ca.client.ClientInterface;
import io.jans.ca.client.RpClient;
import io.jans.ca.common.Jackson2;
import io.jans.ca.common.params.GetRpParams;
import io.jans.ca.common.params.RemoveSiteParams;
import io.jans.ca.common.response.GetRpResponse;
import io.jans.ca.common.response.RemoveSiteResponse;
import io.jans.ca.server.persistence.service.PersistenceService;
import io.jans.ca.server.service.ConfigurationService;
import io.jans.ca.server.service.Rp;
import io.jans.ca.server.service.RpService;
import io.jans.ca.server.service.RpSyncService;
import io.jans.util.Pair;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.NotAuthorizedException;
import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * @author yuriyz
 */

public class Cli {

    public static void main(String[] args) {
        CommandLineParser parser = new DefaultParser();
        CommandLine cmd = null;
        String rpId;
        switchOffLogging();
        try {
            cmd = parser.parse(options(), args);

            rpId = cmd.getOptionValue("rp_id");

            Injector injector = ServerLauncher.getInjector();

            final RpServerConfiguration conf = parseConfiguration(cmd.getOptionValue("c"));
            injector.getInstance(ConfigurationService.class).setConfiguration(conf);
            injector.getInstance(PersistenceService.class).create();

            RpService rpService = injector.getInstance(RpService.class);
            RpSyncService rpSyncService = injector.getInstance(RpSyncService.class);
            rpService.load();

            //check multiple options
            if (hasMultipleActionOptions(cmd)) {
                System.out.println("Multiple parameters in command is not allowed.");
                printHelpAndExit();
                return;
            }
            // list
            if (cmd.hasOption("l")) {
                if (hasListParameterValue(args)) {
                    System.out.println("Warning: Arguments after list parameter is not required, hence will be ignored.");
                }
                final Collection<Rp> values = rpService.getRps().values();
                if (values.isEmpty()) {
                    System.out.println("There are no any entries yet in database.");
                    return;
                }

                System.out.println("rp_id                                client_name");
                for (Rp rp : values) {
                    System.out.println(String.format("%s  %s", rp.getRpId(), rp.getClientName() != null ? rp.getClientName() : ""));
                }
                return;
            }

            // view by oxd_id
            if (cmd.hasOption("rp_id")) {
                print(rpId, rpSyncService.getRp(rpId));
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
                tryToConnectToRunningRp(cmd);
            } else {
                printHelpAndExit();
            }
        } catch (Throwable e) {
            System.out.println("Failed to run jans_client_api CLI (make sure jans_client_api was run at least one time and database file is created). Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static RpServerConfiguration parseConfiguration(String pathToYaml) throws IOException, ConfigurationException {
        if (StringUtils.isBlank(pathToYaml)) {
            System.out.println("Path to yml configuration file is not specified. Exit!");
            System.exit(1);
        }
        File file = new File(pathToYaml);
        if (!file.exists()) {
            System.out.println("Failed to find yml configuration file. Please check " + pathToYaml);
            System.exit(1);
        }

        DefaultConfigurationFactoryFactory<RpServerConfiguration> configurationFactoryFactory = new DefaultConfigurationFactoryFactory<>();
        ConfigurationFactory<RpServerConfiguration> configurationFactory = configurationFactoryFactory.create(RpServerConfiguration.class, Validators.newValidatorFactory().getValidator(), Jackson.newObjectMapper(), "dw");
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

        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        root.setLevel(ch.qos.logback.classic.Level.OFF);
    }

    private static Pair<Integer, Boolean> getPort(RpServerConfiguration conf) {
        final List<ConnectorFactory> applicationConnectors = ((DefaultServerFactory) conf.getServerFactory()).getApplicationConnectors();
        if (applicationConnectors == null || applicationConnectors.isEmpty()) {
            System.out.println("Failed to fetch port from configuration.");
            return null;
        }
        for (ConnectorFactory connectorFactory : applicationConnectors) { // first look up https
            if (connectorFactory instanceof HttpsConnectorFactory) {
                return new Pair<>(((HttpsConnectorFactory) connectorFactory).getPort(), true);
            }
        }
        for (ConnectorFactory connectorFactory : applicationConnectors) { // then http
            if (connectorFactory instanceof HttpConnectorFactory) {
                return new Pair<>(((HttpsConnectorFactory) connectorFactory).getPort(), false);
            }
        }
        return null;
    }

    private static void tryToConnectToRunningRp(CommandLine cmd) {
        final Injector injector = ServerLauncher.getInjector();
        final RpServerConfiguration conf = injector.getInstance(ConfigurationService.class).get();
        if (conf == null) {
            System.out.println("Failed to load configuration file of jans_client_api.");
            return;
        }

        final Pair<Integer, Boolean> port = getPort(conf);
        if (port == null) {
            System.out.println("Failed to fetch port from configuration.");
            return;
        }

        final String protocol = port.getSecond() ? "https" : "http";
        try {
            final ClientInterface client = RpClient.newTrustAllClient(protocol + "://localhost:" + port.getFirst());
            String authorization = "";

            if (cmd.hasOption("a")) {
                authorization = cmd.getOptionValue("a");
            }

            if (StringUtils.isBlank(authorization)) {
                System.out.println("Failed to connect to running jans_client_api. There are two ways to proceed: \n" +
                        " - 1) stop jans_client_api and then run command again. Then script can connect to database directly. If jans_client_api is running it has exclusive lock on database, so script is not able to connect to database directly\n" +
                        " - 2) provide authorization access_token (same that is provided in Authorization header in jans_client_api API) via -a parameter (e.g. lsca.sh -a xxxx-xxxx-xxxx-xxxx -l), so script can connect to running jans_client_api");
                return;
            }
            authorization = "Bearer " + authorization;
            if (cmd.hasOption("l")) {
                GetRpParams params = new GetRpParams();
                params.setList(true);

                String respString = client.getRp(authorization, null, params);
                GetRpResponse resp = Jackson2.createJsonMapper().readValue(respString, GetRpResponse.class);
                if (resp == null) {
                    System.out.println("Failed to fetch entries from database. Please check jans_client_api.log file for details.");
                    return;
                }
                if (resp.getNode() instanceof ArrayNode) {
                    final ArrayNode arrayNode = (ArrayNode) resp.getNode();
                    if (arrayNode.size() == 0) {
                        System.out.println("There are no any entries yet in database.");
                        return;
                    }

                    Iterator<JsonNode> elements = arrayNode.iterator();
                    System.out.println("rp_id                                client_name");
                    while (elements.hasNext()) {
                        final JsonNode element = elements.next();
                        final JsonNode rpIdNode = element.get("rp_id");
                        final JsonNode clientNameNode = element.get("client_name");
                        System.out.println(String.format("%s  %s", rpIdNode != null ? rpIdNode.asText() : "", clientNameNode != null ? clientNameNode.asText() : "null"));
                    }
                } else {
                    System.out.println(resp.getNode());
                }
                return;
            }

            if (cmd.hasOption("rp_id")) {
                final String rpId = cmd.getOptionValue("rp_id");

                String respString = client.getRp(authorization, null, new GetRpParams(rpId));
                GetRpResponse resp = Jackson2.createJsonMapper().readValue(respString, GetRpResponse.class);
                if (resp != null) {
                    print(rpId, resp.getNode());
                } else {
                    System.out.println("Failed to fetch entry from database, please check rp_id really exist and is not malformed (more details at jans_client_api.log file).");
                }
                return;
            }

            if (cmd.hasOption("d")) {
                RemoveSiteResponse resp = client.removeSite(authorization, null, new RemoveSiteParams(cmd.getOptionValue("d")));
                if (resp != null && StringUtils.isNotBlank(resp.getRpId())) {
                    System.out.println("Entry removed successfully.");
                } else {
                    System.out.println("Failed to remove entry from database, please check rp_id really exists and is not malformed (more details in jans_client_api.log file).");
                }
                return;
            }
            printHelpAndExit();
        } catch (BadRequestException e) {
            System.out.println("Bad Request : 400. Failed to execute command against jans_client_api on port " + port + ". Please check rp_id or access_token really exists and is not malformed, error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (NotAuthorizedException e) {
            System.out.println("Not Authorized : 401. Failed to execute command against jans_client_api on port " + port + ". Please check rp_id or access_token really exists and is not malformed, error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (ForbiddenException e) {
            System.out.println("Forbidden : 403. Failed to execute command against jans_client_api on port " + port + ". Please check rp_id or access_token really exists and is not malformed, error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } catch (Exception e) {
            System.out.println("Failed to execute command against jans_client_api on port " + port + ". Please check rp_id or access_token really exists and is not malformed, error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static String sanitizeOutput(String str) {
        return StringUtils.remove(str, "\"");
    }

    private static void print(String rpId, Object rp) {
        if (rp != null) {
            System.out.println("JSON for rp_id " + rpId);
            System.out.println(rp);
        } else {
            System.out.println("No record found in database for rp_id: " + rpId);
        }
    }

    private static Options options() {
        Options options = new Options();

        Option rpIdOption = new Option("rp_id", "rp_id", true, "rp_id is unique identifier within oxd database (returned by register_site and setup_client commands)");
        rpIdOption.setRequired(false);
        options.addOption(rpIdOption);

        Option listOption = new Option("l", "list", false, "lists all rp_ids contained in oxd database");
        listOption.setRequired(false);
        options.addOption(listOption);

        Option deleteOption = new Option("d", "delete", true, "deletes entry from database by rp_id");
        deleteOption.setRequired(false);
        options.addOption(deleteOption);

        Option configOption = new Option("c", "config", true, "path to yml configuration file");
        configOption.setRequired(true);
        options.addOption(configOption);

        Option authorizationOption = new Option("a", "authorization", true, "authorization access_token used to connect to running oxd");
        authorizationOption.setRequired(false);
        options.addOption(authorizationOption);

        return options;
    }

    private static boolean hasMultipleActionOptions(CommandLine cmd) {
        int optionsCount = 0;
        if (cmd.hasOption("l")) {
            optionsCount++;
        }
        if (cmd.hasOption("rp_id")) {
            optionsCount++;
        }
        if (cmd.hasOption("d")) {
            optionsCount++;
        }
        return optionsCount > 1;
    }

    private static boolean hasListParameterValue(String[] args) {
        List<Option> options = new ArrayList<>(options().getOptions());
        options.remove(options().getOption("l"));

        int listIndex = Arrays.asList(args).indexOf("-l");

        if ((args.length - 1) > listIndex) {
            return !options.stream().anyMatch(opt -> "-".concat(opt.getOpt()).equals(args[listIndex + 1]));
        }
        return false;
    }

}