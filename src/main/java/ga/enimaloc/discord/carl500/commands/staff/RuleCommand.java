package ga.enimaloc.discord.carl500.commands.staff;

import ga.enimaloc.discord.carl500.Carl500;
import ga.enimaloc.discord.carl500.constant.Constant;
import ga.enimaloc.discord.carl500.utils.UserUtils;
import ga.enimaloc.discord.commands.Category;
import ga.enimaloc.discord.commands.Command;
import ga.enimaloc.discord.commands.CommandClient;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class RuleCommand implements Command {

    private Options options = new Options()
            .addOption(Option.builder("a")
                    .longOpt("add")
                    .type(String.class)
                    .hasArgs()
                    .valueSeparator('|')
                    .numberOfArgs(Option.UNLIMITED_VALUES)
                    .argName("rules")
                    .desc("Ajouter des règles, options: <règles>")
                    .build())
            .addOption(Option.builder("r")
                    .longOpt("remove")
                    .type(String.class)
                    .hasArgs()
                    .valueSeparator('|')
                    .numberOfArgs(Option.UNLIMITED_VALUES)
                    .argName("rules")
                    .desc("Supprimer des règles, options: <all | numéro de la règles>")
                    .build());

    @Override
    public String getName() {
        return "rule";
    }

    @Override
    public Options getArguments() {
        return options;
    }

    @Override
    public String getHelp() {
        return "Ajouter|Enlever une règle";
    }

    @Override
    public Category getCategory() {
        return Category.getOrCreate("Staff");
    }

    @Override
    public void executeGuild(GuildMessageReceivedEvent event, CommandLine arguments, CommandClient commandClient) {
        if (!event.isWebhookMessage() && UserUtils.isAdministrator(Objects.requireNonNull(event.getMember()))) {
            List<String> rules = this.read();
            if (arguments.hasOption("add")) rules = this.add(arguments.getOptionValues("add"));
            else if (arguments.hasOption("remove")) rules = this.remove(arguments.getOptionValues("remove"));

            this.refresh(rules, event);
            this.write(rules);
        }
    }

    private List<String> add(String[] arguments) {
        List<String> rules = this.read();
        rules.addAll(Arrays.asList(String.join(" ", arguments).replaceFirst("rule add", "").trim().split("\\|")));
        return rules;
    }

    private List<String> remove(String[] arguments) {
        List<String> rules = this.read();
        String args = String.join(" ", arguments).replaceFirst("rule remove", "").trim();
        if ("all".equalsIgnoreCase(args)) {
            rules.clear();
        } else {
            for (String s : args.split(" ")) {
                int i = Integer.parseInt(s) - 1;
                if (rules.size() > i) {
                    rules.remove(i);
                }
            }
        }

        return rules;
    }

    private void refresh(List<String> rules, GuildMessageReceivedEvent event) {
        TextChannel rulesChannel = event.getGuild().getTextChannelById(Constant.CHANNEL_RULES);
        Objects.requireNonNull(rulesChannel).getHistory().retrievePast(50).queue(
                (messages) -> messages.forEach(
                        (m) -> m.delete().queue()
                )
        );
        StringBuilder stringBuilder = new StringBuilder("Pour avoir une bonne entente entre les membres du serveur nous avons instaurez un règlement, celle-ci sont considérées comme accepté par tout les membres au premier message envoyé.\n\n**Règle de " + event.getGuild().getName() + ":**\n\n");

        for(int i = 0; i < rules.size(); ++i) {
            stringBuilder.append("[").append(i + 1).append("] `").append(rules.get(i)).append("`\n");
        }

        stringBuilder.append("\n").append("La modération se donne le droit de modifier les règles à tout moment en vous informant.\n").append("En cas de non-respect de ces règles, un dit « Administrateur » pourra s’octroyer le droit, ").append("sans avis préalable, de prendre les sanctions nécessaires\n").append("Bonne continuation sur ").append(event.getGuild().getName()).append(".");
        rulesChannel.sendMessage(stringBuilder).queueAfter(10L, TimeUnit.SECONDS);
    }

    private void write(List<String> lines) {
        BufferedWriter writer = null;

        try {
            File rulesFile = new File("rules.txt");
            writer = new BufferedWriter(new FileWriter(rulesFile));
            for (String line : lines) {
                writer.write(line);
                writer.newLine();
            }
        } catch (IOException e) {
            Carl500.logger.error("An exception was thrown when trying to create/edit `rule.txt` file", e);
        } finally {
            try {
                assert writer != null;

                writer.close();
            } catch (Exception ignored) {}

        }

    }

    private List<String> read() {
        File ruleFile = new File("rules.txt");
        if (!ruleFile.exists()) {
            return new ArrayList<>();
        } else {
            try {
                return Files.readAllLines(ruleFile.toPath());
            } catch (IOException e) {
                Carl500.logger.error("An exception was thrown when trying to read `rule.txt` file", e);
                return new ArrayList<>();
            }
        }
    }
}
