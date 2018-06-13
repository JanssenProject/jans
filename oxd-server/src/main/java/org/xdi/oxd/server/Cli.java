package org.xdi.oxd.server;

import com.google.inject.Injector;
import org.apache.commons.cli.*;
import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.xdi.oxd.client.CommandClient;
import org.xdi.oxd.common.Command;
import org.xdi.oxd.common.CommandType;
import org.xdi.oxd.common.params.GetRpParams;
import org.xdi.oxd.common.response.GetRpResponse;
import org.xdi.oxd.server.persistence.PersistenceService;
import org.xdi.oxd.server.service.ConfigurationService;
import org.xdi.oxd.server.service.RpService;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * @author yuriyz
 */
public class Cli {

    public static void main(String[] args) {

        Options options = new Options();

        Option input = new Option("oxd_id", "oxd_id", true, "oxd_id is unique identifier within oxd database (returned by register_site and setup_client commands)");
        input.setRequired(true);
        options.addOption(input);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;
        String oxdId = null;
        switchOffLogging();
        try {
            cmd = parser.parse(options, args);
            oxdId = cmd.getOptionValue("oxd_id");

            Injector injector = ServerLauncher.getInjector();

            injector.getInstance(ConfigurationService.class).load();
            injector.getInstance(PersistenceService.class).create();

            RpService rpService = injector.getInstance(RpService.class);
            rpService.load();

            print(oxdId, rpService.getRp(oxdId));
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("utility-name", options);
            System.exit(1);
        } catch (RuntimeException e) {
            // oxd is running and keeps h2 database locked, so we connect to oxd-server and fetch RP via client connection
            tryToConnectToRunningOxd(oxdId);
        } catch (Throwable e) {
            System.out.println("Failed to run oxd CLI (make sure oxd-server was run at least one time and database file is created). Error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void switchOffLogging() {
        List<Logger> loggers = Collections.<Logger>list(LogManager.getCurrentLoggers());
        loggers.add(LogManager.getRootLogger());
        for (Logger logger : loggers) {
            logger.setLevel(Level.OFF);
        }
    }

    private static void tryToConnectToRunningOxd(String oxdId) {
        CommandClient client = null;
        int port = 8099;
        try {
            port = ServerLauncher.getInjector().getInstance(ConfigurationService.class).get().getPort();
            client = new CommandClient("localhost", port);

            final Command command = new Command(CommandType.GET_RP);
            command.setParamsObject(new GetRpParams(oxdId));

            GetRpResponse resp = client.send(command).dataAsResponse(GetRpResponse.class);
            print(oxdId, resp.getRp());
        } catch (IOException e) {
            System.out.println("Failed to get RP from oxd-server on port " + port + ", error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        } finally {
            CommandClient.closeQuietly(client);
        }
    }

    private static void print(String oxdId, Object rp) {
        if (rp != null) {
            System.out.println("JSON for oxd_id " + oxdId);
            System.out.println(rp);
        } else {
            System.out.println("No record found in database for oxd_id: " + oxdId);
        }

    }
}
