package ga.enimaloc.discord.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public interface Command {
    String getName();

    default String[] getAliases() {
        return new String[0];
    }

    default String getArguments() {
        return "";
    }

    default String getHelp() {
        return "";
    }

    default Category getCategory() {
        return Category.get("Uncategorized");
    }

    default void execute(MessageReceivedEvent event, String[] arguments, CommandClient commandClient) {}

    default void executePrivate(PrivateMessageReceivedEvent event, String[] arguments, CommandClient commandClient) {}

    default void executeGuild(GuildMessageReceivedEvent event, String[] arguments, CommandClient commandClient) {}
}