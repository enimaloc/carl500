package ga.enimaloc.discord.carl500.listeners;

import ga.enimaloc.discord.carl500.constant.Constant;
import ga.enimaloc.discord.carl500.constant.Emote;
import ga.enimaloc.discord.carl500.entities.User;
import ga.enimaloc.discord.commands.CommandClient;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.sql.Connection;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Listener extends ListenerAdapter {
    private final CommandClient commandClient;
    private final Connection connection;
    private final Map<Predicate<MessageReactionAddEvent>, Consumer<MessageReactionAddEvent>> waitReaction = new HashMap<>();

    public Listener(CommandClient commandClient, Connection connection) {
        this.commandClient = commandClient;
        this.connection = connection;
    }

    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent event) {
        Matcher matcher = Message.JUMP_URL_PATTERN.matcher(event.getMessage().getContentRaw());
        if (!matcher.find() || event.getMessage().isSuppressedEmbeds()) return;
        TextChannel textChannel = event.getJDA().getTextChannelById(matcher.group(2));
        if (textChannel == null) return;
        textChannel.getHistoryAround(matcher.group(3), 1).queue(history->{
            Message targetMessage = history.getMessageById(matcher.group(3));
            if (targetMessage == null) return;
            EmbedBuilder builder = new EmbedBuilder()
                    .setAuthor(targetMessage.getAuthor().getAsTag(), targetMessage.getAuthor().getEffectiveAvatarUrl())
                    .setTimestamp(targetMessage.isEdited() ? targetMessage.getTimeEdited() : targetMessage.getTimeCreated())
                    .setFooter(targetMessage.isEdited() ? "Edited" : "")
                    .setDescription(targetMessage.getContentRaw());

            for (Message.Attachment attachment : targetMessage.getAttachments()) {

                if (attachment.isImage())
                    builder.setImage(attachment.getUrl());
                else
                    builder.addField(attachment.isVideo() ? "Vidéo" : "Inconnu", String.format("[%s](%s)", attachment.getFileName(), attachment.getUrl()), true);
            }

            event.getChannel().sendMessage(builder.build()).queue();
        });
        super.onGuildMessageReceived(event);
    }
}
