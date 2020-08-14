package ga.enimaloc.discord.carl500;

import ga.enimaloc.discord.carl500.commands.staff.RuleCommand;
import ga.enimaloc.discord.carl500.commands.staff.SayCommand;
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
import java.util.*;
import javax.security.auth.login.LoginException;

import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
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

        logger.trace("Driver found ! ("+driver+")");

        try {
            logger.trace("Trying to connect database (jdbc:{}://{}:{}/{}{}, with user: {})",
                    engine,
                    address,
                    port,
                    databaseName,
                    options.length == 0 ? "" : "?" + String.join("&", options),
                    username
            );
            return DriverManager.getConnection(
                    String.format(
                            "jdbc:%s://%s:%s/%s%s",
                            engine,
                            address,
                            port,
                            databaseName,
                            options.length == 0 ? "" : "?" + String.join("&", options)
                    ),
                    username,
                    password
            );
        } catch (SQLException exception) {
            logger.error("Cannot connect to database", exception);
            logger.error("Exiting...");
            System.exit(1);
            return null; // Unreachable statement
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
        Category.getOrCreate("Général")
                .setDescription("Commande utilisable partout et par tout le monde")
                .setValidator(event -> true)
                .setColor(Color.BLUE);

        logger.trace("Setting up `Staff` category");
        Category.getOrCreate("Staff").setDescription("Commande faites pour le staff, uniquement exécutable sur le serveur")
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
                        new RuleCommand(),
                        new SayCommand()
                )
                .setHelpCommand(new HelpCommand() {

                    @Override
                    public Options getArguments() {
                        return HelpCommand.super.getArguments()
                                .addOption("C", "category", true, "Sélectionner une catégorie a afficher")
                                .addOption("c", "command", true, "Sélectionner une commande a afficher")
                                .addOption("h", "here", false, "Envoyer la liste de commande ici");
                    }

                    public Category getCategory() {
                        return Category.getOrCreate("Général");
                    }

                    @Override
                    public void execute(MessageReceivedEvent event, CommandLine arguments, CommandClient commandClient) {
                        Map<Category, List<Command>> commands = new HashMap<>();
                        commandClient.getCommands().forEach(command -> {
                            List<Command> commandList = commands.getOrDefault(command.getCategory(), new ArrayList<>());
                            commandList.add(command);
                            commands.put(command.getCategory(), commandList);
                        });

                        StringBuilder stringBuilder = new StringBuilder("```apache\n");
                        if (arguments.hasOption("category")) {
                            Category category = Category.get(arguments.getOptionValue("category"));
                            if (category == null) {
                                event.getChannel()
                                        .sendMessageFormat("La catégorie `%s` n'existe pas", arguments.getOptionValue("category")).queue();
                                return;
                            }

                            stringBuilder.append(buildCategory(commands, category, getCommandsMaxSpace(commands.get(category))));
                        } else if (arguments.hasOption("command")) { // TODO: 14/08/2020 Fix that
                            Command command = commandClient.getCommands().stream().filter(cmd -> cmd.getName().equalsIgnoreCase(arguments.getOptionValue("command"))).findFirst().orElse(null);
                            if (command == null) {
                                event.getChannel()
                                        .sendMessageFormat("La commande `%s` n'existe pas", arguments.getOptionValue("command")).queue();
                                return;
                            }

                            stringBuilder.append(buildCommand(command, getOptionsMaxSpace(command.getArguments().getOptions())));
                        } else {
                            for (Category category : commands.keySet()) {
                                stringBuilder.append(buildCategory(commands, category, getCommandsMaxSpace(commandClient.getCommands())));
                            }
                        }
                        MessageChannel channel = arguments.hasOption("here") ? event.getChannel() : event.getAuthor().openPrivateChannel().complete();
                        channel.sendMessageFormat(stringBuilder.append("```").toString()).queue();
                    }

                    private String repeat(int n, String s) {
                        if (n < 0) n = -n;
                        return new String(new char[n]).replace("\0", s);
                    }

                    private StringBuilder buildCategory(Map<Category, List<Command>> commands, Category category, int spacing) {
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append("<Catégorie /")
                                .append(category.getName())
                                .append("/>\n\tl\"")
                                .append(category.getDescription())
                                .append("\"\n\n");
                        StringBuilder stringBuilderCommand;
                        for (Command command : commands.get(category)) {
                            stringBuilderCommand = new StringBuilder()
                                    .append("\t")
                                    .append(command.getName());
                            for (Option option : command.getArguments().getOptions()) {
                                stringBuilderCommand.append(option.isRequired() ? " <" : " [")
                                        .append("-")
                                        .append(option.getOpt())
                                        .append(option.hasArgName() ? option.hasOptionalArg() ? " [" : " <" : "")
                                        .append(option.hasArgName() ? option.getArgName() : "")
                                        .append(option.hasArgName() ? option.hasOptionalArg() ? "]" : ">" : "")
                                        .append(option.isRequired() ? ">" : "]");
                            }
                            stringBuilderCommand.append(repeat(stringBuilderCommand.toString().replaceFirst("\t", "").length() - spacing, " "))
                                    .append("| ")
                                    .append(checkLength(command.getHelp(), spacing))
                                    .append(" []\n");
                            stringBuilder.append(stringBuilderCommand);
                        }
                        stringBuilder.append("</Catégorie>\n\n");
                        return stringBuilder;
                    }

                    private StringBuilder buildCommand(Command command, int spacing) {
                        StringBuilder stringBuilder = new StringBuilder()
                                .append("<Commande /")
                                .append(command.getName())
                                .append("/>\n\tl\"")
                                .append(command.getHelp())
                                .append("\"\n\n");
                        StringBuilder stringBuilderOptions;
                        for (Option option : command.getArguments().getOptions()) {
                            stringBuilderOptions = new StringBuilder()
                                    .append("\t")
                                    .append("-")
                                    .append(option.getOpt())
                                    .append(repeat(option.getOpt().length() - spacing, " "))
                                    .append("|")
                                    .append("\n\t  ")
                                    .append("--")
                                    .append(option.getLongOpt())
                                    .append(option.hasArgName() ? option.hasOptionalArg() ? " [" : " <" : "")
                                    .append(option.hasArgName() ? option.getArgName() : "")
                                    .append(option.hasArgName() ? option.hasOptionalArg() ? "]" : ">" : "");

                            stringBuilderOptions.append(repeat(stringBuilderOptions.toString().split("\\|")[1].replaceFirst("\n\t", "").length() - spacing, " "))
                                    .append("| ")
                                    .append(option.isRequired() ? "Requis. " : "")
                                    .append(checkLength(option.getDescription(), spacing))
                                    .append(" []\n");
                            stringBuilder.append(stringBuilderOptions);
                        }
                        stringBuilder.append("</Commande>\n\n");
                        return stringBuilder;
                    }

                    private int getCommandsMaxSpace(List<Command> commands) {
                        int spacing = 0;
                        StringBuilder stringBuilder;
                        for (Command command : commands) {
                            stringBuilder = new StringBuilder()
                                    .append(command.getName());
                            for (Option option : command.getArguments().getOptions()) {
                                stringBuilder.append(option.isRequired() ? " " : " [")
                                        .append("-")
                                        .append(option.getOpt())
                                        .append(option.hasArgName() ? option.hasOptionalArg() ? " [" : " <" : "")
                                        .append(option.hasArgName() ? option.getArgName() : "")
                                        .append(option.hasArgName() ? option.hasOptionalArg() ? "]" : ">" : "")
                                        .append(option.isRequired() ? " " : "] ");
                            }
                            if (spacing < stringBuilder.toString().length()) spacing = stringBuilder.toString().length();
                        }
                        return spacing;
                    }

                    private int getOptionsMaxSpace(Collection<Option> options) {
                        int spacing = 0;
                        for (Option option : options) {
                            if (spacing < option.getOpt().length()) spacing = option.getOpt().length();
                            if (spacing < option.getLongOpt().length()) spacing = option.getLongOpt().length();
                        }
                        return spacing;
                    }

                    private String checkLength(String s, int spacing) {
                        if (s.length() > 60) {
                            int i = 60;
                            while (s.charAt(i) != ' ') i--;
                            char[] chars = s.toCharArray();
                            chars[i] = '\n';
                            String[] split = new String(chars).split("\n");
                            s = split[0]+"\n\t"+repeat(spacing, " ")+"|   "+checkLength(split[1], spacing);
                        }
                        return s;
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
