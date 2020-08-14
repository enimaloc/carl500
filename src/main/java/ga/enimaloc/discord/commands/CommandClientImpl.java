package ga.enimaloc.discord.commands;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;

public class CommandClientImpl extends ListenerAdapter implements CommandClient {
    private final String[] prefix;
    private final long[] ownersId;
    private final Map<String, Integer> commandsId;
    private final List<Command> commands;
    private final boolean needPrivateInPrivate;
    private final BiConsumer<MessageReceivedEvent, CommandClient> onCommandExecuted;
    private final BiConsumer<GuildMessageReceivedEvent, CommandClient> onGuildCommandExecuted;
    private final BiConsumer<PrivateMessageReceivedEvent, CommandClient> onPrivateCommandExecuted;

    public CommandClientImpl(String[] prefix, long[] ownersId, Map<String, Integer> commandsId, List<Command> commands, boolean needPrivateInPrivate, BiConsumer<MessageReceivedEvent, CommandClient> onCommandExecuted, BiConsumer<GuildMessageReceivedEvent, CommandClient> onGuildCommandExecuted, BiConsumer<PrivateMessageReceivedEvent, CommandClient> onPrivateCommandExecuted) {
        this.prefix = prefix;
        this.ownersId = ownersId;
        this.commandsId = commandsId;
        this.commands = commands;
        this.needPrivateInPrivate = needPrivateInPrivate;
        this.onCommandExecuted = onCommandExecuted;
        this.onGuildCommandExecuted = onGuildCommandExecuted;
        this.onPrivateCommandExecuted = onPrivateCommandExecuted;
    }

    public void onMessageReceived(@Nonnull MessageReceivedEvent event) {
        BiConsumer<Command, String[]> success = (command, arguments) -> {
            if (command.getCategory().getValidator().test(event)) {
                try {
                    command.execute(event, new DefaultParser().parse(command.getArguments(), arguments), this);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                this.onCommandExecuted.accept(event, this);
            }
        };

        if (event.isFromGuild() && this.needPrivateInPrivate) this.check(event.getMessage().getContentRaw(), success);
        else this.execute(event.getMessage().getContentRaw(), success);

        super.onMessageReceived(event);
    }

    public void onPrivateMessageReceived(@Nonnull PrivateMessageReceivedEvent event) {
        BiConsumer<Command, String[]> success = (command, arguments) -> {
            if (command.getCategory().getPrivateValidator().test(event)) {
                try {
                    command.executePrivate(event, new DefaultParser().parse(command.getArguments(), arguments), this);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                this.onPrivateCommandExecuted.accept(event, this);
            }
        };

        if (this.needPrivateInPrivate)  this.check(event.getMessage().getContentRaw(), success);
        else this.execute(event.getMessage().getContentRaw(), success);

        super.onPrivateMessageReceived(event);
    }

    public void onGuildMessageReceived(@Nonnull GuildMessageReceivedEvent event) {
        this.check(event.getMessage().getContentRaw(), (command, arguments) -> {
            if (command.getCategory().getGuildValidator().test(event)) {
                try {
                    command.executeGuild(event, new DefaultParser().parse(command.getArguments(), arguments), this);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                this.onGuildCommandExecuted.accept(event, this);
            }
        });

        super.onGuildMessageReceived(event);
    }

    private void check(String message, BiConsumer<Command, String[]> onSuccess) {
        for (String prefix : this.prefix) {
            if (message.startsWith(prefix)) execute(message, onSuccess);
        }
    }

    private void execute(String message, BiConsumer<Command, String[]> onSuccess) {
        for (String prefix : this.prefix) {
            if (message.startsWith(prefix)) message = message.replaceFirst(prefix, "");
        }

        String[] tMessage = message.split(" ");
        if (commandsId.containsKey(tMessage[0]))
            onSuccess.accept(commands.get(commandsId.get(tMessage[0])), tMessage);

    }

    public String[] getPrefix() {
        return this.prefix;
    }

    public long[] getOwnersId() {
        return this.ownersId;
    }

    public List<Command> getCommands() {
        return this.commands;
    }
}
