package ga.enimaloc.discord.carl500.entities;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import ga.enimaloc.discord.carl500.Carl500;
import ga.enimaloc.discord.carl500.entities.Case.Type;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dv8tion.jda.api.JDA;
import org.slf4j.impl.Logger;
import org.slf4j.impl.StaticLoggerBinder;

public class User {
    private static final Map<Long, User> singleton = new HashMap();
    private static final Logger logger = StaticLoggerBinder.getSingleton().getLoggerFactory().getLogger(User.class);
    private final long userId;
    private final JDA jda;
    private List<Case> cases;
    private long ticketId;

    public static User get(net.dv8tion.jda.api.entities.User user, Connection connection, JDA jda) {
        if (!singleton.containsKey(user.getIdLong())) {
            load(user, connection, jda);
        }

        return singleton.getOrDefault(user.getIdLong(), null);
    }

    public static void load(net.dv8tion.jda.api.entities.User user, Connection connection, JDA jda) {
        try {
            logger.trace("Loading " + user + " from database");
            logger.trace("Select `case` and `ticket_id` where `user_id` = " + user.getIdLong() + " from `users`");
            PreparedStatement statement = connection.prepareStatement("SELECT cases, ticket_id FROM users WHERE user_id = ?;");
            statement.setLong(1, user.getIdLong());
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                singleton.put(user.getIdLong(), new User(user.getIdLong(), (new Gson()).fromJson(resultSet.getString("cases"), (new TypeToken<ArrayList<Case>>() {
                }).getType()), resultSet.getLong("ticket_id"), jda));
            } else {
                try {
                    PreparedStatement insert = connection.prepareStatement("INSERT INTO users (user_id) VALUES (?)");
                    insert.setLong(1, user.getIdLong());
                    insert.executeUpdate();
                    load(user, connection, jda);
                } catch (SQLException var6) {
                    Carl500.logger.error("An exception was thrown when trying to insert ` with `user_id` is `" + user.getIdLong() + "` in `users` table", var6);
                }
            }
        } catch (SQLException var7) {
            Carl500.logger.error("An exception was thrown when trying to select `cases` and `ticket_id` where `user_id` is `" + user.getIdLong() + "` in `users` table", var7);
        }

    }

    public User(long userId, List<Case> cases, long ticketId, JDA jda) {
        this.userId = userId;
        this.cases = cases;
        this.ticketId = ticketId;
        this.jda = jda;
    }

    public net.dv8tion.jda.api.entities.User getUser() {
        return (net.dv8tion.jda.api.entities.User)this.jda.retrieveUserById(this.userId).complete();
    }

    public long getTicketId() {
        return this.ticketId;
    }

    public void setTicketId(long ticketId) {
        this.ticketId = ticketId;
    }

    public List<Case> getCases() {
        return this.cases;
    }

    public void createCase(long author, Type type, String reason) {
        this.createCase(new Case(this.userId, author, type, reason));
    }

    public void createCase(Case caze) {
        this.cases.add(caze);
    }
}
