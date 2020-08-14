package ga.enimaloc.discord.commands;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.apache.commons.cli.CommandLine;

public interface HelpCommand extends Command {
    default String getName() {
        return "help";
    }

    default String[] getAliases() {
        return new String[0];
    }

    default String getHelp() {
        return "Display help page";
    }

    default Category getCategory() {
        return Category.getOrCreate("General");
    }

    default void execute(MessageReceivedEvent event, CommandLine arguments, CommandClient commandClient) {
        Map<Category, List<Command>> map = new HashMap<>();

        for(Command command : commandClient.getCommands()) {
            List<Command> list = map.getOrDefault(command.getCategory(), new ArrayList<>());
            list.add(command);
            map.put(command.getCategory(), list);
        }

        map.forEach((category, commands) -> {
            EmbedBuilder embedBuilder = new EmbedBuilder()
                    .setColor(category.getColor())
                    .setTitle(category.getName())
                    .setDescription(category.getDescription());

            for(Command command : commands) {
                embedBuilder.addField(command.getName() + " " + command.getArguments(), command.getHelp(), true);
            }

            event.getAuthor().openPrivateChannel().queue((privateChannel) -> privateChannel.sendMessage(embedBuilder.build()).queue());
        });
    }
}
