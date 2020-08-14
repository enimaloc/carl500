package ga.enimaloc.discord.carl500.commands.staff;

import ga.enimaloc.discord.commands.Category;
import ga.enimaloc.discord.commands.Command;
import ga.enimaloc.discord.commands.CommandClient;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;

public class SayCommand implements Command {

    private final Options options;

    public SayCommand() {
        options = new Options()
                .addOption("t", "target", true, "Cible ou est envoyé le message(utilisateur ou salon)")
                .addOption("m", "message", true, "Message as envoyé");
    }

    @Override
    public String getName() {
        return "say";
    }

    @Override
    public Options getArguments() {
        return options;
    }

    @Override
    public String getHelp() {
        return "Envoyée un message à l'utilisateur/dans le salon spécifié, si aucun spécifiée envoie dans le salon ou est fait la commande";
    }

    @Override
    public Category getCategory() {
        return Category.getOrCreate("Staff");
    }

    @Override
    public void executeGuild(GuildMessageReceivedEvent event, CommandLine arguments, CommandClient commandClient) {

    }
}
