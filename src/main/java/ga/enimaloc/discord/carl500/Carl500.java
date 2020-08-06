package ga.enimaloc.discord.carl500;

import ga.enimaloc.discord.carl500.commands.staff.RuleCommand;
import ga.enimaloc.discord.carl500.constant.Constant;
import ga.enimaloc.discord.carl500.constant.Default;
import ga.enimaloc.discord.carl500.constant.Info;
import ga.enimaloc.discord.carl500.listeners.Listener;
import ga.enimaloc.discord.carl500.utils.UserUtils;
import ga.enimaloc.discord.commands.Category;
import ga.enimaloc.discord.commands.Command;
import ga.enimaloc.discord.commands.CommandClient;
import ga.enimaloc.discord.commands.CommandClientBuilder;
import ga.enimaloc.discord.commands.HelpCommand;
import java.awt.Color;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import javax.security.auth.login.LoginException;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.commons.cli.CommandLine;
import org.slf4j.impl.Logger;
import org.slf4j.impl.StaticLoggerBinder;

public class Carl500 {

    public static Logger logger = StaticLoggerBinder.getSingleton().getLoggerFactory().getLogger(Carl500.class);
    private final CommandLine arguments;
    private final Connection connection;

    public Carl500(CommandLine arguments) {
        logger.info("Starting Carl500, version " + Info.VERSION);

        this.arguments = arguments;
        this.connection = this.setupJDBC(
                arguments.getOptionValue("SQLDriver", Default.SQL_DRIVER),
                arguments.getOptionValue("SQLEngine", Default.SQL_ENGINE),
                arguments.getOptionValue("SQLAddress", Default.SQL_ADDRESS),
                arguments.getOptionValue("SQLPort", Default.SQL_PORT),
                arguments.getOptionValue("SQLDatabaseName", Default.SQL_DATABASE),
                arguments.hasOption("SQLOptions") ? arguments.getOptionValues("SQLOptions") : Default.SQL_OPTIONS,
                arguments.getOptionValue("SQLUsername", Default.SQL_USERNAME),
                arguments.getOptionValue("SQLPassword")
        );
        this.generateSQL();
        this.setupJDA();
        this.setupCategory();
    }

    private Connection setupJDBC(String driver, String engine, String address, String port, String databaseName, String[] options, String username, String password) {
        try {
            Class.forName(driver);
        } catch (ClassNotFoundException exception) {
            logger.error("Cannot found JDBC Driver", exception);
            logger.error("Exiting...");
            System.exit(1);
        }

        try {
            return DriverManager.getConnection(String.format("jdbc:%s://%s:%s/%s%s", engine, address, port, databaseName, options.length == 0 ? "" : "?" + String.join("&", options)), username, password);
        } catch (SQLException exception) {
            logger.error("Cannot connect to Database", exception);
            logger.error("Exiting...");
            System.exit(1);
            return null;
        }
    }

    private void generateSQL() {
        try {
            logger.debug("Trying to create `users` table");
            this.connection.createStatement().execute(
                    "CREATE TABLE IF NOT EXISTS `users` (" +
                            "`id` INT NOT NULL AUTO_INCREMENT," +
                            "`user_id` BIGINT NOT NULL," +
                            "`cases` JSON," +
                            "`ticket_id` BIGINT DEFAULT 0," +
                            "PRIMARY KEY (`id`)" +
                        ");");
        } catch (SQLException exception) {
            logger.error("An exception was thrown when trying to create `users` table", exception);
        }

    }

    private void setupCategory() {
        logger.trace("Setting up category");
        logger.trace("Setting up `Général` category");
        Category.get("Général")
                .setDescription("Commande utilisable partout et par tout le monde")
                .setValidator(event -> true)
                .setColor(Color.BLUE);

        logger.trace("Setting up `Staff` category");
        Category.get("Staff").setDescription("Commande faites pour le staff, uniquement exécutable sur le serveur")
                .setValidator(event -> false)
                .setPrivateValidator(event -> false)
                .setGuildValidator(event -> event.getMember() != null && UserUtils.isAdministrator(event.getMember()))
                .setColor(Color.ORANGE);
    }

    private void setupJDA() {
        logger.trace("Setting up JDA");
        logger.trace("Setting up command client");
        CommandClient commandClient = new CommandClientBuilder()
                .setPrefix(this.arguments.getOptionValue("prefix", Info.inDev ? "b500!" : "c500!"))
                .setOwnersId(
                        this.arguments.hasOption("ownersIds") ?
                                Arrays.stream(this.arguments.getOptionValues("ownersIds")).mapToLong(Long::parseLong).toArray() :
                                Constant.OWNERS_IDS)
                .setCommands(
                        // Admin command
                        new RuleCommand()
                )
                .setHelpCommand(new HelpCommand() {
                    public Category getCategory() {
                        return Category.get("Général");
                    }
                })
                .setNeedPrefixInPrivate(true)
                .build();

        logger.trace("Setting up JDABuilder");
        JDABuilder jdaBuilder = JDABuilder
                .createDefault(this.arguments.getOptionValue("token"))
                .addEventListeners(commandClient, new Listener(commandClient, this.connection))
                .enableIntents(GatewayIntent.GUILD_MEMBERS);

        try {
            logger.trace("Build JDA client");
            jdaBuilder.build();
        } catch (LoginException exception) {
            logger.error("An exception was throw when JDA was build", exception);
            logger.error("Exiting...");
            System.exit(1);
        }

    }
}
