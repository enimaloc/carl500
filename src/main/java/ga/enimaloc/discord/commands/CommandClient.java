package ga.enimaloc.discord.commands;

import java.util.List;

public interface CommandClient {
    String[] getPrefix();

    long[] getOwnersId();

    List<Command> getCommands();
}
