package ga.enimaloc.discord.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public interface Command {
    String getName();

    default String[] getAliases() {
        return new String[0];
    }

    default Options getArguments() {
        return new Options();
    }

    default String getHelp() {
        return "";
    }

    default Category getCategory() {
        return Category.getOrCreate("Uncategorized");
    }

    default void execute(MessageReceivedEvent event, CommandLine arguments, CommandClient commandClient) {}

    default void executePrivate(PrivateMessageReceivedEvent event, CommandLine arguments, CommandClient commandClient) {}

    default void executeGuild(GuildMessageReceivedEvent event, CommandLine arguments, CommandClient commandClient) {}
}
