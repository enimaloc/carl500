package ga.enimaloc.discord.commands;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;

public class Category {
    private static final Map<String, Category> singleton = new HashMap();

    private final String name;
    private String description;
    private Color color;
    private Predicate<MessageReceivedEvent> validator = event -> true;
    private Predicate<GuildMessageReceivedEvent> guildValidator = event -> true;
    private Predicate<PrivateMessageReceivedEvent> privateValidator = event -> true;

    public static Category get(String name) {
        if (!singleton.containsKey(name)) singleton.put(name, new Category(name));
        return singleton.get(name);
    }

    Category(String name) {
        this.name = name;
    }

    public Category setDescription(String description) {
        this.description = description;
        return this;
    }

    public Category setColor(Color color) {
        this.color = color;
        return this;
    }

    public Category setValidator(Predicate<MessageReceivedEvent> validator) {
        this.validator = validator;
        return this;
    }

    public Category setGuildValidator(Predicate<GuildMessageReceivedEvent> guildValidator) {
        this.guildValidator = guildValidator;
        return this;
    }

    public Category setPrivateValidator(Predicate<PrivateMessageReceivedEvent> privateValidator) {
        this.privateValidator = privateValidator;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public String getDescription() {
        return this.description;
    }

    public Color getColor() {
        return this.color;
    }

    public Predicate<MessageReceivedEvent> getValidator() {
        return this.validator;
    }

    public Predicate<GuildMessageReceivedEvent> getGuildValidator() {
        return this.guildValidator;
    }

    public Predicate<PrivateMessageReceivedEvent> getPrivateValidator() {
        return this.privateValidator;
    }

    @Override
    public String toString() {
        return "Category{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", color=" + color +
                ", validator=" + validator +
                ", guildValidator=" + guildValidator +
                ", privateValidator=" + privateValidator +
                '}';
    }
}
