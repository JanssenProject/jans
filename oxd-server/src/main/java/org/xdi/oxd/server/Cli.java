package org.xdi.oxd.server;

import com.google.inject.Injector;
import io.dropwizard.configuration.ConfigurationException;
import io.dropwizard.configuration.ConfigurationFactory;
import io.dropwizard.configuration.DefaultConfigurationFactoryFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import org.apache.commons.cli.*;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.xdi.oxd.server.persistence.PersistenceService;
import org.xdi.oxd.server.service.ConfigurationService;
import org.xdi.oxd.server.service.RpService;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
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
                for (String oxdIdKey : rpService.getRps().keySet()) {
                    System.out.println(oxdIdKey);
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
            System.out.println("Failed to open database file, make sure oxd-server is stopped. Otherwise it locks database and it is not possible to open it.");
            e.printStackTrace();
            printHelpAndExit();
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
