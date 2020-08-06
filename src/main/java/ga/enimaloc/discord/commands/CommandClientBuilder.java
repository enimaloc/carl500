package ga.enimaloc.discord.commands;

import java.io.Serializable;
import java.util.*;
import java.util.function.BiConsumer;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public class CommandClientBuilder {

    private String[] prefix;
    private long[] ownersId;
    private final Map<String, Integer> commandsId = new HashMap<>();
    private final List<Command> commands = new ArrayList<>();
    private HelpCommand helpCommand = new HelpCommand() {};
    private boolean needPrefixInPrivate = false;
    private BiConsumer<MessageReceivedEvent, CommandClient> onCommandExecuted = null;
    private BiConsumer<GuildMessageReceivedEvent, CommandClient> onGuildCommandExecuted = null;
    private BiConsumer<PrivateMessageReceivedEvent, CommandClient> onPrivateCommandExecuted = null;

    public CommandClientBuilder setPrefix(String... prefix) {
        this.prefix = prefix;
        return this;
    }

    public CommandClientBuilder setOwnersId(long... ownersId) {
        this.ownersId = ownersId;
        return this;
    }

    public CommandClientBuilder setCommands(Command... commands) {
        this.setCommands(Arrays.asList(commands));
        return this;
    }

    public CommandClientBuilder setCommands(List<Command> commands) {
        this.commandsId.clear();
        this.commands.clear();
        commands.forEach(this::addCommand);
        return this;
    }

    public CommandClientBuilder addCommand(Command command) {
        List<String> list = Arrays.asList(command.getAliases());
        list.add(command.getName());
        if (commandsId.keySet().stream().anyMatch(list::contains))
            throw new IllegalArgumentException("A command with the same name or aliases is already registered");

        commands.add(command);
        for (String name : list) commandsId.put(name, commands.size()-1);
        return this;
    }

    public CommandClientBuilder setHelpCommand(HelpCommand helpCommand) {
        this.helpCommand = helpCommand;
        return this;
    }

    public CommandClientBuilder setNeedPrefixInPrivate(boolean needPrefixInPrivate) {
        this.needPrefixInPrivate = needPrefixInPrivate;
        return this;
    }

    public CommandClientBuilder onCommandExecuted(BiConsumer<MessageReceivedEvent, CommandClient> onCommandExecuted) {
        this.onCommandExecuted = onCommandExecuted;
        return this;
    }

    public CommandClientBuilder onGuildCommandExecuted(BiConsumer<GuildMessageReceivedEvent, CommandClient> onGuildCommandExecuted) {
        this.onGuildCommandExecuted = onGuildCommandExecuted;
        return this;
    }

    public CommandClientBuilder onPrivateCommandExecuted(BiConsumer<PrivateMessageReceivedEvent, CommandClient> onPrivateCommandExecuted) {
        this.onPrivateCommandExecuted = onPrivateCommandExecuted;
        return this;
    }

    public CommandClient build() {
        this.addCommand(this.helpCommand);
        return new CommandClientImpl(this.prefix, this.ownersId, this.commandsId, this.commands, this.needPrefixInPrivate, this.onCommandExecuted, this.onGuildCommandExecuted, this.onPrivateCommandExecuted);
    }
}