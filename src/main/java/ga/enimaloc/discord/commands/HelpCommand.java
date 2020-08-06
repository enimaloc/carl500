package ga.enimaloc.discord.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

public interface HelpCommand extends Command {
    default String getName() {
        return "help";
    }

    default String[] getAliases() {
        return new String[0];
    }

    default String getArguments() {
        return "";
    }

    default String getHelp() {
        return "Display help page";
    }

    default Category getCategory() {
        return Category.get("General");
    }

    default void execute(MessageReceivedEvent event, String[] arguments, CommandClient commandClient) {
        Map<Category, List<Command>> map = new HashMap();
        Iterator var5 = commandClient.getCommands().iterator();

        while(var5.hasNext()) {
            Command command = (Command)var5.next();
            List<Command> list = map.getOrDefault(command.getCategory(), new ArrayList());
            list.add(command);
            map.put(command.getCategory(), list);
        }

        map.forEach((category, commands) -> {
            EmbedBuilder embedBuilder = (new EmbedBuilder()).setColor(category.getColor()).setTitle(category.getName()).setDescription(category.getDescription());
            Iterator var4 = commands.iterator();

            while(var4.hasNext()) {
                Command command = (Command)var4.next();
                embedBuilder.addField(command.getName() + " " + command.getArguments(), command.getHelp(), true);
            }

            event.getAuthor().openPrivateChannel().queue((privateChannel) -> {
                privateChannel.sendMessage(embedBuilder.build()).queue();
            });
        });
    }
}
