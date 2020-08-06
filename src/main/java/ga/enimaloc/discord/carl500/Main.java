package ga.enimaloc.discord.carl500;

import ga.enimaloc.discord.carl500.constant.Default;
import ga.enimaloc.discord.carl500.constant.Info;
import io.sentry.Sentry;
import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

import io.sentry.SentryClient;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.event.Level;
import org.slf4j.impl.StaticLoggerBinder;

public class Main {
    public static CommandLine arguments;


    public static void main(String[] args) {
        String jarName = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()).getName();
        Options options = new Options()
                    .addOption("h", "help", false, "Display this page")
                    .addOption(
                            Option.builder("env")
                                    .longOpt("environment")
                                    .hasArg()
                                    .argName("development | production")
                                    .numberOfArgs(1)
                                    .type(String.class)
                                    .desc("Set environment, allowed value 'development' or 'production' ["+Default.ENVIRONMENT+"]")
                                    .build()
                    )
                    .addOption("dSentry", "disableSentry", false, "Disable sentry service")
                    .addOption(
                            Option.builder("dns")
                                    .longOpt("sentryDns")
                                    .hasArg()
                                    .argName("dns link")
                                    .numberOfArgs(1)
                                    .type(String.class)
                                    .desc("DNS of sentry service")
                                    .required()
                                    .build()
                    )
                    .addOption(
                            Option.builder("logLevel")
                                    .longOpt("loggerLevel")
                                    .hasArg()
                                    .argName("level")
                                    .numberOfArgs(1)
                                    .type(String.class)
                                    .desc("Set logger level, allowed value 'all', 'trace', 'debug', 'info', 'warn', 'error', 'off' ["+Default.LOGGER_LEVEL+"]")
                                    .build()
                    )
                    .addOption(
                            Option.builder("token")
                                    .hasArg()
                                    .argName("token")
                                    .numberOfArgs(1)
                                    .type(String.class)
                                    .desc("Required. Discord bot token")
                                    .required()
                                    .build()
                    )
                    .addOption(
                            Option.builder("prefix")
                                    .hasArg()
                                    .argName("prefix")
                                    .numberOfArgs(1)
                                    .type(String.class)
                                    .desc("Override default bot prefix [dev ? "+Default.PREFIX_DEV+" : "+Default.PREFIX_PROD+"]")
                                    .build()
                    )
                    .addOption(
                            Option.builder("owners")
                                    .longOpt("ownersIds")
                                    .hasArg()
                                    .argName("Owners Ids")
                                    .numberOfArgs(-2)
                                    .valueSeparator(';')
                                    .type(String[].class)
                                    .desc("Override default owners ids registered in database, but not replace")
                                    .build()
                    )
                    .addOption(
                            Option.builder("SQLDriver")
                                    .hasArg()
                                    .argName("driver path")
                                    .numberOfArgs(1)
                                    .type(String.class)
                                    .desc("Override default SQL driver ["+Default.SQL_DRIVER+"]")
                                    .build()
                    )
                    .addOption(
                            Option.builder("SQLEngine")
                                    .hasArg()
                                    .argName("engine")
                                    .numberOfArgs(1)
                                    .type(String.class)
                                    .desc("Override default SQL engine ["+Default.SQL_ENGINE+"]")
                                    .build()
                    )
                    .addOption(
                            Option.builder("SQLIp")
                                    .longOpt("SQLAddress")
                                    .hasArg()
                                    .argName("address")
                                    .numberOfArgs(1)
                                    .type(String.class)
                                    .desc("Override default SQL address ["+Default.SQL_ADDRESS+"]")
                                    .build()
                    )
                    .addOption(
                            Option.builder("SQLPort")
                                    .hasArg()
                                    .argName("prefix")
                                    .numberOfArgs(1)
                                    .type(String.class)
                                    .desc("Override default SQL port ["+Default.SQL_PORT+"]")
                                    .build()
                    )
                    .addOption(
                            Option.builder("SQLDatabase")
                                    .longOpt("SQLDatabaseName")
                                    .hasArg()
                                    .argName("databaseName")
                                    .numberOfArgs(1)
                                    .type(String.class)
                                    .desc("Override default SQL database name ["+Default.SQL_DATABASE+"]")
                                    .build()
                    )
                    .addOption(
                            Option.builder("SQLUser")
                                    .longOpt("SQLUsername")
                                    .hasArg()
                                    .argName("username")
                                    .numberOfArgs(1)
                                    .type(String.class)
                                    .desc("Override default SQL username ["+Default.SQL_USERNAME+"]")
                                    .build()
                    )
                    .addOption(
                            Option.builder("SQLPass")
                                    .longOpt("SQLPassword")
                                    .hasArg()
                                    .argName("password")
                                    .numberOfArgs(1)
                                    .type(String.class)
                                    .desc("Required. SQL password used to connect to database")
                                    .required()
                                    .build()
                    );

        HelpFormatter helpFormatter = new HelpFormatter();

        try {
            arguments = new DefaultParser().parse(options, args);
            List<String> validOptions = Arrays.asList("development", "production");
            if (arguments.hasOption("environment") && !validOptions.contains(arguments.getOptionValue("environment").toLowerCase())) {
                throw new ParseException("environment option need one of this value: " + String.join(", ", validOptions));
            }

            validOptions = Arrays.asList("all", "trace", "debug", "info", "warn", "error", "off");
            if (arguments.hasOption("loggerLevel") && !validOptions.contains(arguments.getOptionValue("loggerLevel").toLowerCase())) {
                throw new ParseException("loggerLevel option need one of this value: " + String.join(", ", validOptions));
            }
        } catch (ParseException var5) {
            System.err.println("Failed to parse (Cause: " + var5.getLocalizedMessage() + ")");
            helpFormatter.printHelp("java -jar " + jarName, options, true);
            System.exit(1);
        }

        if (arguments.hasOption("help")) {
            helpFormatter.printHelp("java -jar " + jarName, options, true);
            System.exit(0);
        }

        StaticLoggerBinder.getSingleton().getLoggerFactory().getLogger("ROOT").setLevel(Level.valueOf(arguments.getOptionValue("loggerLevel", "info").toUpperCase()));

        if (!arguments.hasOption("disableSentry")) {
            Sentry.init(arguments.getOptionValue("sentryDns", null));
            Sentry.getStoredClient().setEnvironment(arguments.getOptionValue("environment", "production"));
            Sentry.getStoredClient().setRelease(Info.VERSION);
            try {
                Sentry.getStoredClient().setServerName(InetAddress.getLocalHost().getCanonicalHostName());
            } catch (UnknownHostException e) {
                Carl500.logger.error("Cannot get localhost hostname", e);
            }
        }

        new Carl500(arguments);
    }
}