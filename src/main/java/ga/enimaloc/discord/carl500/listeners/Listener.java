package ga.enimaloc.discord.carl500.listeners;

import ga.enimaloc.discord.carl500.constant.Emote;
import ga.enimaloc.discord.carl500.entities.User;
import ga.enimaloc.discord.commands.CommandClient;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.Connection;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class Listener extends ListenerAdapter {
    private final CommandClient commandClient;
    private final Connection connection;
    private final Map<Predicate<MessageReactionAddEvent>, Consumer<MessageReactionAddEvent>> waitReaction = new HashMap();

    public Listener(CommandClient commandClient, Connection connection) {
        this.commandClient = commandClient;
        this.connection = connection;
    }

    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent event) {
        if (Arrays.stream(this.commandClient.getPrefix()).anyMatch((prefix) -> event.getMessage().getContentRaw().startsWith(prefix)) ||
                event.getAuthor().isBot())
            return;
        if (User.get(event.getAuthor(), this.connection, event.getJDA()).getTicketId() != 0L)
            Objects.requireNonNull(event.getJDA().getTextChannelById(event.getAuthor().getId())).sendMessage(event.getMessage()).queue();
        else this.createTicketProcedure(event);

        super.onPrivateMessageReceived(event);
    }

    private void createTicketProcedure(PrivateMessageReceivedEvent event) {
        event.getChannel().sendMessage("Voulez vous envoyer votre précédant message au staff de `Stationeers-FR` ?\nRéagissez avec "+Emote.CONFIRMATION+" pour oui ou "+Emote.CANCEL+" pour non").queue((message) -> {
            message.addReaction(Emote.CONFIRMATION).queue();
            message.addReaction(Emote.CANCEL).queue();
            this.waitReaction.put(
                    (check) ->
                            check.isFromType(ChannelType.PRIVATE) && check.getUserIdLong() == event.getAuthor().getIdLong() &&
                            check.getReactionEmote().isEmoji() &&
                            Arrays.asList(Emote.CONFIRMATION, Emote.CANCEL).contains(check.getReactionEmote().getEmoji()),
                    (e) -> {
                        switch (e.getReactionEmote().getEmoji()) {
                            case Emote.CONFIRMATION:
                                Objects.requireNonNull(e.getJDA().getTextChannelById(501840622538457098L)).sendMessage(
                                        new EmbedBuilder()
                                                .setDescription(event.getMessage().getContentRaw() + "\n\n" +
                                                        "Réagissez avec " + Emote.TICKET_ACCEPT + " pour créer un nouveaux salon pour parler avec la personne\n" +
                                                        "Réagissez avec " + Emote.TICKET_DENY + " pour supprimer le ticket\n" +
                                                        "Réagissez avec " + Emote.TICKET_REPORT + " pour signalé un abus")
                                                .setTimestamp(new Date().toInstant())
                                                .setColor(Color.RED)
                                                .build()
                                ).queue((msg) -> {
                                    msg.addReaction(Emote.TICKET_ACCEPT).queue();
                                    msg.addReaction(Emote.TICKET_DENY).queue();
                                    msg.addReaction(Emote.TICKET_REPORT).queue();
                                    e.getPrivateChannel().sendMessage("Votre ticket vient d'être envoyé au staff").queue();
                                    User.get(event.getAuthor(), this.connection, e.getJDA());
                                }, RestAction.getDefaultFailure().andThen((throwable) -> {
                                    e.getPrivateChannel().sendMessage("Votre ticket n'as pas pus être envoyé au staff, l'erreur a été reporter à mon développeur, veuillez ne pas recréez de ticket pendant ce temps").queue();
                                }));
                                break;

                            case Emote.CANCEL:
                                e.getPrivateChannel().sendMessage("Votre ticket vient d'être annulé").queue();
                                break;
                        }
                    });
        });
    }

    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        this.waitReaction.keySet().stream().filter(predicate -> predicate.test(event)).iterator()
                .forEachRemaining(predicate -> this.waitReaction.remove(predicate).accept(event));
        super.onMessageReactionAdd(event);
    }
}
